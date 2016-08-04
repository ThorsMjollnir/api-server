package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CodeError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError
import uk.ac.ncl.openlab.intake24.AssociatedFoodWithHeader
import uk.ac.ncl.openlab.intake24.services.fooddb.user.AssociatedFoodsService

trait AssociatedFoodsAdminService {
  
  def associatedFoods(foodCode: String, locale: String): Either[CodeError, Seq[AssociatedFoodWithHeader]]
  def updateAssociatedFoods(foodCode: String, locale: String, associatedFoods: Seq[AssociatedFood]): Either[UpdateError, Unit]
}
