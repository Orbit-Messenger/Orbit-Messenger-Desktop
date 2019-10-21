package com.orbitmessenger

import com.orbitmessenger.Controllers.LoginController
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage

class Main : Application() {

    @Throws(Exception::class)
    override fun start(primaryStage: Stage) {
        val loader = FXMLLoader(javaClass.getResource("./FXML/loginView.fxml"))
        val loginController = LoginController()
        loader.setController(loginController)
        val root = loader.load() as Parent
        primaryStage.icons.add(Image("com/orbitmessenger/images/orbit.png"))
        primaryStage.title = "Orbit Messenger"
        primaryStage.scene = Scene(root)
        primaryStage.show()
    }

    companion object {


        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(*args)
        }
    }
}
