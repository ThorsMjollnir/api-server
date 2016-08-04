package uk.ac.ncl.openlab.intake24.foodsql.admin

import anorm._
import anorm.SqlParser.str
import anorm.NamedParameter.symbol
import uk.ac.ncl.openlab.intake24.foodsql.SqlDataService
import uk.ac.ncl.openlab.intake24.FoodGroupRecord
import uk.ac.ncl.openlab.intake24.FoodGroupMain
import uk.ac.ncl.openlab.intake24.FoodGroupLocal
import uk.ac.ncl.openlab.intake24.services.admin.FoodGroupsAdminService

trait FoodGroupsAdminServiceImpl extends FoodGroupsAdminService with SqlDataService {
  private case class FoodGroupRow(id: Long, description: String, local_description: Option[String])

  def allFoodGroups(locale: String): Either[DatabaseError, Seq[FoodGroupRecord]] = tryWithConnection {
    implicit conn =>
      SQL("""|SELECT id, description, local_description 
             |FROM food_groups 
             |  LEFT JOIN food_groups_local ON food_groups_local.food_group_id = food_groups.id AND food_groups_local.locale_id = {locale_id}""".stripMargin)
        .on('locale_id -> locale)
        .executeQuery()
        .as(Macro.namedParser[FoodGroupRow].*)
        .map(r => FoodGroupRecord(FoodGroupMain(r.id.toInt, r.description), FoodGroupLocal(r.local_description)))
  }

  def foodGroup(id: Int, locale: String): Option[FoodGroupRecord] = tryWithConnection {
    implicit conn =>
      SQL("""|SELECT description, local_description 
                        |FROM food_groups 
                        |     LEFT JOIN food_groups_local ON food_groups_local.food_group_id = food_groups.id AND food_groups_local.locale_id = {locale_id}
                        |WHERE id = {id}""".stripMargin)
        .on('id -> id, 'locale_id -> locale)
        .executeQuery()
        .as((str("description") ~ str("local_description").?).singleOpt)
        .map(desc => FoodGroupRecord(FoodGroupMain(id, desc._1), FoodGroupLocal(desc._2)))
  }

  def deleteAllFoodGroups() = tryWithConnection {
    implicit conn =>
      SQL("DELETE FROM food_groups").execute()
  }

  def createFoodGroups(foodGroups: Seq[FoodGroupMain]) = tryWithConnection {
    implicit conn =>
      if (foodGroups.nonEmpty) {
        val foodGroupParams =
          foodGroups.map(g => Seq[NamedParameter]('id -> g.id, 'description -> g.englishDescription))

        BatchSql("""INSERT INTO food_groups VALUES ({id}, {description})""", foodGroupParams).execute()

      }
  }

  def deleteLocalFoodGroups(locale: String) = tryWithConnection {
    implicit conn =>
      SQL("DELETE FROM food_groups_locale WHERE locale_id={locale_id}")
        .on('locale_id -> locale)
        .execute()
  }

  def createLocalFoodGroups(localFoodGroups: Map[Int, String], locale: String) = tryWithConnection {
    implicit conn =>

      val foodGroupLocalParams =
        localFoodGroups.flatMap {
          case (id, localDescription) =>
            Seq[NamedParameter]('id -> id, 'locale_id -> locale, 'local_description -> localDescription)
        }.toSeq

      BatchSql("""INSERT INTO food_groups_local VALUES ({id}, {locale_id}, {local_description})""", foodGroupLocalParams)
        .execute()
  }

}