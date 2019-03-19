package com.example

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.html.*
import kotlinx.html.*
import kotlinx.css.*
import freemarker.cache.*
import io.ktor.freemarker.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.netty.channel.MessageSizeEstimator
import java.util.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }

    val client = HttpClient(Apache) {
    }

    val boards = ArrayList<MessageBoards>()

    //Instanciation boards par défaut
    boards.add(MessageBoards("Cryptomonnaies"))
    boards.add(MessageBoards("Kotlin"))
    boards.add(MessageBoards("Swift"))

    routing {
        get("/boards") {
            call.respondHtml {
                body {
                    h1 { +"Liste des boards" }
                    ul {
                        for (n in boards) {
                            li {
                                a("boards/"+n.id) { +"${n.name}" }
                            }
                        }
                    }
                    form("/form", encType = FormEncType.multipartFormData, method = FormMethod.post) {
                        acceptCharset = "utf-8"
                        p {
                            label { +"Nom du board: " }
                            textInput { name = "name" }
                        }
                        p {
                            submitInput { value = "Ajouter" }
                        }
                    }
                }
            }
        }

        post("/form") {
            val multipart = call.receiveMultipart()
            val part = multipart.readPart()
            when (part) {
                is PartData.FormItem ->
                    boards.add(MessageBoards(part.value))
            }
            call.respondRedirect("/boards")
        }

        get("/boards/{id}") {
            val id = call.parameters["id"]
            var actualBoard : MessageBoards = MessageBoards("")
            for (i in boards) {
                if (id?.toInt() == i.id) {
                    actualBoard = i
                }
            }


            call.respondHtml {
                body {
                    a("/boards") { +"<- Go back" }
                    h1 { + "${actualBoard.name}" }
                    div {
                        for (i in actualBoard.messages) {
                            p { +"${i.user} à ${i.date} : " }
                            p { +"${i.message}" }
                        }
                    }
                    form("/boards/$id/form", encType = FormEncType.multipartFormData, method = FormMethod.post) {
                        acceptCharset = "utf-8"
                        p {
                            label { +"Pseudo: " }
                            textInput { name = "pseudo" }
                        }
                        p {
                            label { +"Message: " }
                            textInput { name = "message" }
                        }
                        p {
                            submitInput { value = "Envoyer" }
                        }
                    }
                }
            }
        }

        post("/boards/{id}/form") {
            var message : String = ""
            var pseudo : String = ""
            val id = call.parameters["id"]
            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "pseudo") {
                            pseudo = part.value
                        }

                        if (part.name == "message") {
                            message = part.value
                        }
                    }
                }
            }

            var actualBoard = MessageBoards("test")
            for (i in boards) {
                if (id?.toInt() == i.id) {
                    actualBoard = i
                }
            }
            actualBoard.messages.add(Messages(message, pseudo))
            call.respondRedirect("/boards/$id")
        }

        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        get("/html-dsl") {
            call.respondHtml {
                body {
                    h1 { +"HTML" }
                    ul {
                        for (n in 1..10) {
                            li { +"$n" }
                        }
                    }
                }
            }
        }

        get("/styles.css") {
            call.respondCss {
                body {
                    backgroundColor = Color.red
                }
                p {
                    fontSize = 2.em
                }
                rule("p.myclass") {
                    color = Color.blue
                }
            }
        }

        get("/html-freemarker") {
            call.respond(FreeMarkerContent("index.ftl", mapOf("data" to IndexData(listOf(1, 2, 3))), ""))
        }
    }
}

data class IndexData(val items: List<Int>)

class MessageBoards(name: String?) {
    val name = name
    val id = (0..1000).random()
    val messages = ArrayList<Messages>()
}

class Messages(message: String?, user: String?, date: Date = Date()) {
    val message = message
    val user = user
    val date = date.hours.toString() + "h" + date.minutes.toString()
}

fun FlowOrMetaDataContent.styleCss(builder: CSSBuilder.() -> Unit) {
    style(type = ContentType.Text.CSS.toString()) {
        +CSSBuilder().apply(builder).toString()
    }
}

fun CommonAttributeGroupFacade.style(builder: CSSBuilder.() -> Unit) {
    this.style = CSSBuilder().apply(builder).toString().trim()
}

suspend inline fun ApplicationCall.respondCss(builder: CSSBuilder.() -> Unit) {
    this.respondText(CSSBuilder().apply(builder).toString(), ContentType.Text.CSS)
}
