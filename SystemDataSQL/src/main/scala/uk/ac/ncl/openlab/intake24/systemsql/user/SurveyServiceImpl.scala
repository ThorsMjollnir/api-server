package uk.ac.ncl.openlab.intake24.systemsql.user

import javax.inject.{Inject, Named}
import javax.sql.DataSource

import anorm._

import uk.ac.ncl.openlab.intake24.errors._
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.SurveyState
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{PublicSurveyParameters, SurveyService, UserSurveyParameters}
import uk.ac.ncl.openlab.intake24.sql.{SqlDataService, SqlResourceLoader}
import uk.ac.ncl.openlab.intake24.surveydata.NutrientMappedSubmission

class SurveyServiceImpl @Inject()(@Named("intake24_system") val dataSource: DataSource) extends SurveyService with SqlDataService with SqlResourceLoader {

  private case class UserSurveyParametersRow(scheme_id: String, state: Int, locale: String, started: Boolean, finished: Boolean, suspension_reason: Option[String], survey_monkey_url: Option[String], support_email: String)

  override def getPublicSurveyParameters(surveyId: String): Either[LookupError, PublicSurveyParameters] = tryWithConnection {
    implicit conn =>
      SQL("SELECT locale, support_email FROM surveys WHERE id={survey_id}")
        .on('survey_id -> surveyId)
        .executeQuery()
        .as((SqlParser.str("locale") ~ SqlParser.str("support_email")).singleOpt) match {
        case Some(locale ~ email) =>
          Right(PublicSurveyParameters(locale, email))
        case None =>
          Left(RecordNotFound(new RuntimeException(s"Survey $surveyId does not exist")))
      }
  }

  override def getSurveyParameters(surveyId: String): Either[LookupError, UserSurveyParameters] = tryWithConnection {
    implicit conn =>
      SQL("SELECT scheme_id, state, locale, now() >= start_date AS started, now() > end_date AS finished, suspension_reason, survey_monkey_url, support_email FROM surveys WHERE id={survey_id}")
        .on('survey_id -> surveyId)
        .executeQuery()
        .as(Macro.namedParser[UserSurveyParametersRow].singleOpt) match {
        case Some(row) => {

          val state: String = SurveyState.fromCode(row.state) match {
            case SurveyState.Active =>
              (row.started, row.finished) match {
                case (false, _) => "pending"
                case (_, true) => "finished"
                case _ => "running"
              }
            case SurveyState.Suspended =>
              "suspended"
            case SurveyState.NotInitialised =>
              "pending"
          }

          Right(UserSurveyParameters(row.scheme_id, row.locale, state, row.suspension_reason, row.survey_monkey_url, row.support_email))
        }
        case None =>
          Left(RecordNotFound(new RuntimeException(s"Survey $surveyId does not exist")))
      }
  }

  def createSubmission(surveyId: String, userId: String, survey: NutrientMappedSubmission): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        val generatedId = java.util.UUID.randomUUID()

        SQL("INSERT INTO survey_submissions VALUES ({id}::uuid, {survey_id}, {user_id}, {start_time}, {end_time}, ARRAY[{log}])")
          .on('id -> generatedId, 'survey_id -> surveyId, 'user_id -> userId, 'start_time -> survey.startTime,
            'end_time -> survey.endTime, 'log -> survey.log)
          .execute()

        val customFieldParams = survey.customData.map {
          case (name, value) => Seq[NamedParameter]('survey_submission_id -> generatedId, 'name -> name, 'value -> value)
        }.toSeq

        if (!customFieldParams.isEmpty) {
          BatchSql("INSERT INTO survey_submission_custom_fields VALUES (DEFAULT, {survey_submission_id}::uuid, {name}, {value})", customFieldParams.head, customFieldParams.tail: _*).execute()
        }

        // Meals

        if (!survey.meals.isEmpty) {

          val mealParams = survey.meals.map {
            meal =>
              Seq[NamedParameter]('survey_submission_id -> generatedId, 'hours -> meal.time.hours, 'minutes -> meal.time.minutes, 'name -> meal.name)
          }

          val batch = BatchSql("INSERT INTO survey_submission_meals VALUES (DEFAULT, {survey_submission_id}::uuid, {hours}, {minutes}, {name})", mealParams.head, mealParams.tail: _*)

          val mealIds = AnormUtil.batchKeys(batch)

          val meals = mealIds.zip(survey.meals)

          // Custom fields

          val mealCustomFieldParams = meals.flatMap {
            case (meal_id, meal) =>
              meal.customData.map {
                case (name, value) => Seq[NamedParameter]('meal_id -> meal_id, 'name -> name, 'value -> value)
              }
          }

          if (!mealCustomFieldParams.isEmpty) {
            BatchSql("INSERT INTO survey_submission_meal_custom_fields VALUES (DEFAULT, {meal_id}, {name}, {value})", mealCustomFieldParams.head, mealCustomFieldParams.tail: _*).execute()
          }

          // Foods

          val mealFoodsParams = meals.flatMap {
            case (meal_id, meal) =>
              meal.foods.map {
                case food =>
                  Seq[NamedParameter]('meal_id -> meal_id, 'code -> food.code, 'english_description -> food.englishDescription, 'local_description -> food.localDescription, 'ready_meal -> food.isReadyMeal, 'search_term -> food.searchTerm,
                    'portion_size_method_id -> food.portionSize.method, 'reasonable_amount -> food.reasonableAmount, 'food_group_id -> food.foodGroupId, 'food_group_english_description -> food.foodGroupEnglishDescription,
                    'food_group_local_description -> food.foodGroupLocalDescription, 'brand -> food.brand, 'nutrient_table_id -> food.nutrientTableId, 'nutrient_table_code -> food.nutrientTableCode)
              }
          }

          if (!mealFoodsParams.isEmpty) {

            val batch = BatchSql("INSERT INTO survey_submission_foods VALUES (DEFAULT, {meal_id}, {code}, {english_description}, {local_description}, {ready_meal}, {search_term}, {portion_size_method_id}, {reasonable_amount},{food_group_id},{food_group_english_description},{food_group_local_description},{brand},{nutrient_table_id},{nutrient_table_code})",
              mealFoodsParams.head, mealFoodsParams.tail: _*)

            val foodIds = AnormUtil.batchKeys(batch)

            val foods = foodIds.zip(meals.flatMap(_._2.foods))

            // Food custom fields

            val foodCustomFieldParams = foods.flatMap {
              case (food_id, food) =>
                food.customData.map {
                  case (name, value) => Seq[NamedParameter]('food_id -> food_id, 'name -> name, 'value -> value)
                }
            }

            if (!foodCustomFieldParams.isEmpty) {
              BatchSql("INSERT INTO survey_submission_food_custom_fields VALUES (DEFAULT, {food_id}, {name}, {value})", foodCustomFieldParams.head, foodCustomFieldParams.tail: _*).execute()
            }

            // Food portion size method parameters

            val foodPortionSizeMethodParams = foods.flatMap {
              case (food_id, food) =>
                food.portionSize.data.map {
                  case (name, value) => Seq[NamedParameter]('food_id -> food_id, 'name -> name, 'value -> value)
                }
            }

            if (!foodPortionSizeMethodParams.isEmpty) {
              BatchSql("INSERT INTO survey_submission_portion_size_fields VALUES (DEFAULT, {food_id}, {name}, {value})", foodPortionSizeMethodParams.head, foodPortionSizeMethodParams.tail: _*).execute()
            }

            // Food nutrient values

            val foodNutrientParams = foods.flatMap {
              case (food_id, food) =>
                food.nutrients.map {
                  case (nutrientTypeId, value) => Seq[NamedParameter]('food_id -> food_id, 'nutrient_type_id -> nutrientTypeId, 'value -> value)
                }
            }

            if (!foodNutrientParams.isEmpty) {
              BatchSql("INSERT INTO survey_submission_nutrients(id, food_id, nutrient_type_id, amount) VALUES (DEFAULT, {food_id}, {nutrient_type_id}, {value})", foodNutrientParams.head, foodNutrientParams.tail: _*).execute()
            }
          }
        }

        Right(())
      }
  }
}