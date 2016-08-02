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

package controllers

import be.objectify.deadbolt.scala.DeadboltActions
import javax.inject.Inject
import play.api.http.ContentTypes
import play.api.libs.json.JsBoolean
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Result
import security.Roles

import uk.ac.ncl.openlab.intake24.MainCategoryRecord
import uk.ac.ncl.openlab.intake24.LocalCategoryRecord
import uk.ac.ncl.openlab.intake24.MainFoodRecord
import uk.ac.ncl.openlab.intake24.services.AdminFoodDataService
import uk.ac.ncl.openlab.intake24.services.CodeError
import uk.ac.ncl.openlab.intake24.services.InvalidRequest
import uk.ac.ncl.openlab.intake24.services.NewCategory
import uk.ac.ncl.openlab.intake24.services.NewFood
import uk.ac.ncl.openlab.intake24.services.SqlException
import uk.ac.ncl.openlab.intake24.services.Success
import uk.ac.ncl.openlab.intake24.services.UpdateResult
import uk.ac.ncl.openlab.intake24.services.VersionConflict
import uk.ac.ncl.openlab.intake24.LocalFoodRecord
import uk.ac.ncl.openlab.intake24.services.UserFoodDataService
import models.AdminFoodRecord
import models.AdminCategoryRecord

import upickle.default.write
import upickle.default.read 

import uk.ac.ncl.openlab.intake24.AssociatedFoodRecord
import uk.ac.ncl.openlab.intake24.UserFoodHeader
import uk.ac.ncl.openlab.intake24.UserCategoryHeader
import uk.ac.ncl.openlab.intake24.huh.UserAssociatedFood

class AdminFoodDataController @Inject() (service: AdminFoodDataService, userService: UserFoodDataService, deadbolt: DeadboltActions) extends Controller with PickleErrorHandler {

  // Read 
  def rootCategories(locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.rootCategories(locale))).as(ContentTypes.JSON)
    }
  }

  def uncategorisedFoods(locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.uncategorisedFoods(locale))).as(ContentTypes.JSON)
    }
  }

  def categoryContents(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.categoryContents(code, locale))).as(ContentTypes.JSON)
    }
  }

  def foodParentCategories(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.foodParentCategories(code, locale))).as(ContentTypes.JSON)
    }
  }

  def foodAllCategories(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.foodAllCategories(code, locale))).as(ContentTypes.JSON)
    }
  }
  
  case class Bozo(main: MainFoodRecord, local: LocalFoodRecord, brands: Seq[String], associatedFoods: Seq[UserAssociatedFood])
  
  case class Qwe(x: Int)
  
  case class Test (foodOrCategory: Either[UserFoodHeader, UserCategoryHeader], promptText: String, linkAsMain: Boolean, genericName: String)
  
  case class Kozo(local: LocalFoodRecord, main: MainFoodRecord, brands: Seq[String], associatedFoods: Seq[UserAssociatedFood])

  def foodRecord(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      val result = for {
        record <- service.foodRecord(code, locale).right
        brandNames <- userService.brandNames(code, locale).right
        associatedFoods <- userService.associatedFoods(code, locale).right
      } yield AdminFoodRecord(record.main, record.local, brandNames, associatedFoods)
      
      val t = Kozo(null, null, null, null)
      val w = write(t)
      
      result match {
        case Left(CodeError.UndefinedCode) => NotFound
        case Right(record) => Ok(write(record)).as(ContentTypes.JSON)
      }
    }
  }

  def categoryRecord(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      service.categoryRecord(code, locale) match {
        case Left(CodeError.UndefinedCode) => NotFound
        case Right(record) => Ok(write(AdminCategoryRecord(record.main, record.local))).as(ContentTypes.JSON)
      }      
    }
  }

  def categoryParentCategories(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.categoryParentCategories(code, locale))).as(ContentTypes.JSON)
    }
  }

  def categoryAllCategories(code: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.categoryAllCategories(code, locale))).as(ContentTypes.JSON)
    }
  }

  def allAsServedSets() = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.allAsServedSets())).as(ContentTypes.JSON)
    }
  }

  def allDrinkware = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.allDrinkware())).as(ContentTypes.JSON)
    }
  }

  def allGuideImages() = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.allGuideImages())).as(ContentTypes.JSON)
    }
  }

  def allFoodGroups(locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.allFoodGroups(locale))).as(ContentTypes.JSON)
    }
  }

  def foodGroup(id: Int, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      service.foodGroup(id, locale) match {
        case Some(group) => Ok(write(group)).as(ContentTypes.JSON)
        case None => BadRequest
      }
    }
  }

  def searchFoods(searchTerm: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.searchFoods(searchTerm, locale))).as(ContentTypes.JSON)
    }
  }

  def searchCategories(searchTerm: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.searchCategories(searchTerm, locale))).as(ContentTypes.JSON)
    }
  }

  def nutrientTables() = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(write(service.nutrientTables())).as(ContentTypes.JSON)
    }
  }

  // Write

  def translateUpdateResult(result: UpdateResult): Result = result match {
    case Success => Ok
    case VersionConflict => Conflict
    case InvalidRequest(errorCode, message) => BadRequest(Json.obj("error" -> "invalid_request", "code" -> errorCode, "message" -> message))
    case SqlException(message) => InternalServerError(Json.obj("error" -> "sql_exception", "message" -> message))
  }

  def updateFoodBase(foodCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateUpdateResult(service.updateFoodBase(foodCode, read[MainFoodRecord](request.body)))
      }
    }
  }

  def updateFoodLocal(foodCode: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateUpdateResult(service.updateFoodLocal(foodCode, locale, read[LocalFoodRecord](request.body)))
      }
    }
  }

  def updateAssociatedFoodPrompts(foodCode: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateUpdateResult(service.updateAssociatedFoods(foodCode, locale, read[Seq[AssociatedFoodRecord]](request.body)))
      }
    }
  }

  def isFoodCodeAvailable(code: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(JsBoolean(service.isFoodCodeAvailable(code)))
    }
  }

  def createFood() = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateUpdateResult(service.createFood(read[NewFood](request.body)))
      }
    }
  }

  def createFoodWithTempCode() = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        service.createFoodWithTempCode(read[NewFood](request.body)) match {
          case Left(error) => translateUpdateResult(error)
          case Right(tempCode) => Ok(tempCode)
        }
      }
    }
  }

  def deleteFood(code: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateUpdateResult(service.deleteFood(code))
    }
  }

  def addFoodToCategory(categoryCode: String, foodCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateUpdateResult(service.addFoodToCategory(categoryCode, foodCode))

    }
  }
  def removeFoodFromCategory(categoryCode: String, foodCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateUpdateResult(service.removeFoodFromCategory(categoryCode, foodCode))
    }
  }

  def addSubcategoryToCategory(categoryCode: String, subcategoryCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateUpdateResult(service.addSubcategoryToCategory(categoryCode, subcategoryCode))
    }
  }

  def removeSubcategoryFromCategory(categoryCode: String, subcategoryCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateUpdateResult(service.removeSubcategoryFromCategory(categoryCode, subcategoryCode))

    }
  }

  def isCategoryCodeAvailable(code: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      Ok(JsBoolean(service.isCategoryCodeAvailable(code)))
    }
  }

  def createCategory() = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateUpdateResult(service.createCategory(read[NewCategory](request.body)))
      }
    }
  }

  def updateCategoryBase(categoryCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateUpdateResult(service.updateCategoryBase(categoryCode, read[MainCategoryRecord](request.body)))
      }
    }
  }

  def updateCategoryLocal(categoryCode: String, locale: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action(parse.tolerantText) { implicit request =>
      tryWithPickle {
        translateUpdateResult(service.updateCategoryLocal(categoryCode, locale, read[LocalCategoryRecord](request.body)))
      }
    }
  }

  def deleteCategory(categoryCode: String) = deadbolt.Restrict(List(Array(Roles.superuser))) {
    Action {
      translateUpdateResult(service.deleteCategory(categoryCode))
    }
  }
}
