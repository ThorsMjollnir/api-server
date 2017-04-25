/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package controllers.system

import java.time._
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import javax.inject.Inject

import controllers.DatabaseErrorHandler
import parsers.{JsonUtils, SurveyCSVExporter}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{BodyParsers, Controller}
import security.DeadboltActionsAdapter
import uk.ac.ncl.openlab.intake24.api.shared.ErrorDescription
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodGroupsAdminService
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{DataExportService, SurveyAdminService}
import io.circe.generic.auto._
import models.Intake24Subject
import play.api.Logger
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles

import scala.concurrent.Future


class DataExportController @Inject()(service: DataExportService, surveyAdminService: SurveyAdminService, foodGroupsAdminService: FoodGroupsAdminService, deadbolt: DeadboltActionsAdapter) extends Controller
  with DatabaseErrorHandler with JsonUtils {

  def getSurveySubmissions(surveyId: String, dateFrom: String, dateTo: String, offset: Int, limit: Int) = deadbolt.restrictToRoles(Roles.superuser, Roles.surveyStaff(surveyId))(BodyParsers.parse.empty) {
    _ =>
      Future {

        try {
          val parsedFrom = ZonedDateTime.parse(dateFrom)
          val parsedTo = ZonedDateTime.parse(dateTo)

          translateDatabaseResult(service.getSurveySubmissions(surveyId, Some(parsedFrom), Some(parsedTo), offset, limit, None))
        } catch {
          case e: DateTimeParseException => BadRequest(toJsonString(ErrorDescription("DateFormat", "Failed to parse date parameter. Expected a UTC date in ISO 8601 format, e.g. '2017-02-15T16:40:30Z'.")))
        }
      }
  }

  def getMySurveySubmissions(surveyId: String) = deadbolt.restrictToRoles(Roles.surveyRespondent(surveyId))(BodyParsers.parse.empty) {
    request =>
      Future {

        val respondentId = request.subject.get.asInstanceOf[Intake24Subject].userId

        try {
          translateDatabaseResult(service.getSurveySubmissions(surveyId, None, None, 0, Int.MaxValue, Some(respondentId)))
        } catch {
          case e: DateTimeParseException => BadRequest(toJsonString(ErrorDescription("DateFormat", "Failed to parse date parameter. Expected a UTC date in ISO 8601 format, e.g. '2017-02-15T16:40:30Z'.")))
        }
      }
  }

  def getSurveySubmissionsAsCSV(surveyId: String, dateFrom: String, dateTo: String) = deadbolt.restrictToRoles(Roles.superuser, Roles.surveyStaff(surveyId))(BodyParsers.parse.empty) {
    request =>
      Future {

        try {
          val parsedFrom = ZonedDateTime.parse(dateFrom)
          val parsedTo = ZonedDateTime.parse(dateTo)
          val forceBOM = request.getQueryString("forceBOM").isDefined

          val data = for (params <- surveyAdminService.getSurveyParameters(surveyId).right;
                          localNutrients <- surveyAdminService.getLocalNutrientTypes(params.localeId).right;
                          dataScheme <- surveyAdminService.getCustomDataScheme(params.schemeId).right;
                          foodGroups <- foodGroupsAdminService.listFoodGroups(params.localeId).right;
                          submissions <- service.getSurveySubmissions(surveyId, Some(parsedFrom), Some(parsedTo), 0, Integer.MAX_VALUE, None).right) yield ((localNutrients, dataScheme, foodGroups, submissions))

          data match {
            case Right((localNutrients, dataScheme, foodGroups, submissions)) =>
              SurveyCSVExporter.exportSurveySubmissions(dataScheme, foodGroups, localNutrients, submissions, forceBOM ) match {
                case Right(csvFile) =>
                  val dateStamp = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.ofInstant(Clock.systemUTC().instant(), ZoneId.systemDefault).withNano(0)).replace(":", "-").replace("T", "-")

                  Logger.error(csvFile.getAbsolutePath)

                  Ok.sendFile(csvFile, fileName = _ => s"intake24-$surveyId-data-$dateStamp.csv", onClose = () => csvFile.delete()).as(if (forceBOM) "application/octet-stream" else "text/csv;charset=utf-8")
                case Left(exportError) => InternalServerError(toJsonString(ErrorDescription("ExportError", exportError)))
              }
            case Left(databaseError) => translateDatabaseError(databaseError)
          }


        } catch {
          case e: DateTimeParseException => BadRequest(toJsonString(ErrorDescription("DateFormat", "Failed to parse date parameter. Expected a UTC date in ISO 8601 format, e.g. '2017-02-15T16:40:30Z'.")))
        }
      }
  }
}
