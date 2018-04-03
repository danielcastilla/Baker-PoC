import java.util.UUID

import akka.actor.ActorSystem
import com.ing.baker.compiler.RecipeCompiler
import com.ing.baker.recipe.common.FiresOneOfEvents
import com.ing.baker.recipe.scaladsl.{Event, Ingredient, Ingredients, Interaction, Recipe}
import com.ing.baker.runtime.core.Baker

  object Demo {

    def main(args: Array[String]) {
      println(visualization)
    }

    implicit val actorSystem = ActorSystem.create("WebShopActorSystem")
    //Ingredients
    val customerInfo = Ingredient[String]("customerInfo")
    val trackingId = Ingredient[String]("trackingId")
    val order = Ingredient[String]("order")
    val goods = Ingredient[String]("goods")
    val invoiceWasSend = Ingredient[String]("InvoicedWasSend")

    //Events
    val goodsShipped = Event("GoodsShipped", trackingId)
    val orderPlaced = Event("OrderPlaced", order)
    val paymentMade = Event("PaymentMade")
    val customerInfoReceived = Event("CustomerInfoReceived", customerInfo)
    val valid = Event("Valid")
    val sorry = Event("Sorry")
    val goodsManufactured = Event("GoodsManufactured", goods)
    val sendInvoiceSuccessful = Event("SendInvoiceSuccessful", invoiceWasSend)

    //Interactions
    val validateOrder = Interaction(
      name = "ValidateOrder",
      inputIngredients = order,
      output = FiresOneOfEvents(valid, sorry)
    )

    val manufactureGoods = Interaction(
      name = "ManufactureGoods",
      inputIngredients = order,
      output =  FiresOneOfEvents(goodsManufactured)
    )

    val shipGoods = Interaction(
      name = "ShipGoods",
      inputIngredients = Ingredients(goods, customerInfo),
      output =FiresOneOfEvents(goodsShipped)
    )

    val sendInvoice = Interaction(
      name = "SendInvoice",
      inputIngredients = customerInfo,
      output = FiresOneOfEvents(sendInvoiceSuccessful)
    )

    //Recipe
    val webShopRecipe: Recipe =
      Recipe("WebShop")
        .withInteractions(
          validateOrder,
          manufactureGoods
            .withRequiredEvents(valid,paymentMade),
          shipGoods,
          sendInvoice
            .withRequiredEvent(goodsShipped)
        )
        .withSensoryEvents(
          customerInfoReceived,
          orderPlaced,
          paymentMade
        )

    //compiles the recipe
    val compiledRecipe = RecipeCompiler.compileRecipe(webShopRecipe)

    //list of validation error
    val errors: Seq[String] = compiledRecipe.validationErrors

    val visualization: String = compiledRecipe.getRecipeVisualization

  //implementations
    val validateOrderImpl = validateOrder implement{
      (order: String) => {
        valid.instance()
      }
    }

    val manufacturesGoodsImpl = manufactureGoods implement{
      (goods: String) => {
        goodsManufactured.instance()
      }
    }

    val sendInvoiceImpl = sendInvoice implement{
      (customerInfo: String) => {
        sendInvoiceSuccessful.instance()
      }
    }

    val shipGoodsImpl = shipGoods implement{
      (goods: String) =>{
        goodsShipped.instance()
      }
    }

    val implementations = Seq(validateOrderImpl, manufacturesGoodsImpl, sendInvoiceImpl, shipGoodsImpl)

    val baker = new Baker()

    baker.addRecipe(compiledRecipe)
    baker.addInteractionImplementation(implementations)

    val processId = UUID.randomUUID().toString
    val recipeId = UUID.randomUUID().toString
    baker.bake(recipeId, processId)




  }
