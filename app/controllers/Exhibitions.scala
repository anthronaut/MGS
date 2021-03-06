package controllers

import play.api._

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models._
import views._

import anorm._
//import anorm.SqlParser._

import play.api.db._
import play.api.Play.current

import play.mvc.BodyParser;

import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.JsObject

import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.BodyParser;

//import securesocial.core.{Identity, Authorization}
import play.Logger
import java.io.File
import scala.xml.XML
import java.util.zip.{ZipEntry, ZipFile}
import scala.collection.JavaConversions._
object Exhibitions extends Controller with Secured{
	

	def list = IsAuthenticated{user => _ =>
		Ok(views.html.exhibition.listSimple(Exhibition.list(Some(0))))
	}

	def listRaw = IsAuthenticated{user => _ =>
		var r:String = ""

		Exhibition.list().map{l =>
			r = r + {"\t> " * (l.level.toInt-1)} + l.id.get + " " + l.name + "\n" 
		}

		Ok(r)
	}

	def branchRaw(id: Long) = IsAuthenticated{user => _ =>
		var r:String = ""

		Exhibition.branch(id).map{l =>
			r = r + l.name + " > " 
		}

		Ok(r.dropRight(2))
	}

	def add = IsAuthenticated{user => implicit request =>
		Ok(views.html.exhibition.addOrEdit(menuForm(0), Exhibition.subSelect(), Exhibition.types))
	}

	def insert = IsAuthenticated{user => implicit request =>
		menuForm(0).bindFromRequest.fold(
			error => Ok(views.html.exhibition.addOrEdit(error, Exhibition.subSelect(), Exhibition.types)),
			values => {
				val id = Exhibition.insertOrUpdate(values)
				Redirect(routes.Exhibitions.edit(id))
				.flashing(
    				"success" -> "exhibition.created.successfully"
  				)
			}
		)
	}

	def edit(id: Long) = IsAuthenticated{user => implicit request =>
		Ok(views.html.exhibition.addOrEdit(
			menuForm(id).fill(Exhibition.detail(id).get)
			, Exhibition.subSelect()
			, Exhibition.types
			, id
			, Exhibition.detail(id)
			, Some(Exhibition.listSub(id))
			)
		)
	}

	def update(id: Long) = IsAuthenticated{user => implicit request =>
		menuForm(id).bindFromRequest.fold(
			error => Ok(views.html.exhibition.addOrEdit(
							error
							, Exhibition.subSelect()
							, Exhibition.types
							, id
							, Exhibition.detail(id)
			)),
			values => {
				Exhibition.insertOrUpdate(values, id)
				Redirect(routes.Exhibitions.edit(id))
				.flashing(
    				"success" -> "exhibition.edited.successfully"
  				)
			}
		)
	}

	def addSub(id: Long) = IsAuthenticated{user => implicit request =>
		Ok(views.html.exhibition.addCat(catForm(id), Exhibition.subSelect(Some(id)), Exhibition.typesSelect, id))
	}

	def insertSub(id: Long) = Action{implicit request =>
		catForm(id).bindFromRequest.fold(
			error => Ok(views.html.exhibition.addCat(error, Exhibition.subSelect(Some(id)), Exhibition.typesSelect, id)),
			values => {

				val data = Exhibition(NotAssigned, values.name, values.name_en, 0, Some(values.sub.getOrElse(id)), 0, None, None, None, None, true, values.number, values.type_id, None, None, None, None, None)
				Exhibition.insertOrUpdate(data)
				
				Redirect(routes.Exhibitions.edit(id))
				.flashing(
    				"success" -> "cat.created.successfully"
  				)
			}
		)
	}

	def editSub(id: Long, parent_id: Long) = IsAuthenticated{user => implicit request =>

		Ok(views.html.exhibition.editSub(
			id
			, parent_id
			, Exhibition.detail(id).get
			, menuForm(parent_id, id).fill(Exhibition.detail(id).get)
			, Exhibition.typesSelect
			)
		)
	}

	def updateSub(id: Long, parent_id: Long) = IsAuthenticated{user => implicit request =>
		menuForm(parent_id, id).bindFromRequest.fold(
			error => Ok(views.html.exhibition.editSub(
							id
							, parent_id
							, Exhibition.detail(id).get
							, error
							, Exhibition.typesSelect)
			),
			values => {

				Exhibition.insertOrUpdate(values, id)
				Redirect(routes.Exhibitions.editSub(id, parent_id))
				.flashing(
    				"success" -> "cat.edited.successfully"
  				)
			}
		)
	}
	
// QUIZ
	

	 

	def serveQuestions(id: Long) = IsAuthenticated{user => _ =>
	  implicit val quizFormat = Json.format[Quiz]
	  val quizList = Exhibition.serveQuestions(id)
	  val str = Json.obj("questions" -> quizList)
		Ok(str)
	}
	
	def addQuestion(id: Long, question_id: Long) = IsAuthenticated{user => _ =>
		Exhibition.addQuestion(id, question_id)
		Ok("done")
		.flashing(
    		"success" -> "Frage hinzugefügt"
  	)
	}
	
    def updateQuestion(id: Long) = Action { request =>
  	  request.body.asJson.map { json =>
       val question_id = (json \ "pk").toString().replace("\"", "");
        val value = (json \ "value").toString().replace("\"", "");        
        val field = (json \ "name").toString().replace("\"", "");
        Exhibition.updateQuestion(id, question_id, field, value)
        
        Ok(json)
    }.getOrElse {
      BadRequest("Expecting Json data")
    }
	}
	


	
	
				
	def menuForm(id: Long, id2: Long = 0) = Form(
		mapping(
			"id" -> ignored(NotAssigned:Pk[Long]),
			"name" -> optional(text),
			"name_en" -> optional(text),
			"level" -> ignored(1.toLong),
			"sub" -> optional(longNumber),
			"position" -> ignored(1.toLong), 
			"date_begin" -> optional(date("dd.MM.yyyy")),
			"date_end" -> optional(date("dd.MM.yyyy")),
			"comment" -> optional(text),
			"comment_en" -> optional(text),
			"status_id" -> boolean,
			"number" -> optional(longNumber),
			"type_id" -> optional(longNumber),
			"file" -> optional(text),
			"file2" -> optional(text),
			"file3" -> optional(text),
			"width" -> optional(text),
			"height" -> optional(text)
		)(Exhibition.apply)(Exhibition.unapply)
		verifying("form.error.number_already_exists", fields => fields match {
    case a => {
    	if(a.number.isDefined){
    		Exhibition.listSub(id).filter((b: Exhibition) => (b.number.getOrElse(0)==a.number.get && b.id.get!=id2)).size==0
    	}
    	else{
    		true
    	}
  }})
	)

	def catForm(id: Long) = Form(
		mapping(
			"id" -> ignored(NotAssigned:Pk[Long]),
			"name" -> optional(text),
			"name_en" -> optional(text),
			"sub" -> optional(longNumber),
			"type_id" -> optional(longNumber),
			"number" -> optional(longNumber)
		)
		(ExhibitionCat.apply)(ExhibitionCat.unapply)
		verifying("form.error.number_already_exists", fields => fields match {
    case a => {
    	if(a.number.isDefined){
    		Exhibition.listSub(id).filter((b: Exhibition) => b.number.getOrElse(0)==a.number.get).size==0
    	}
    	else{
    		true
    	}
  }})
	)
	

	def tourForm(id: Long, id2: Long = 0) = Form(
		mapping(
			"id" -> ignored(NotAssigned:Pk[Long]),
			"prev" -> optional(longNumber),
			"next" -> optional(longNumber),
			"nextcomment" -> optional(text),
			"tournumber" -> optional(number)		  
		)(Tours.apply)(Tours.unapply))
				

	def move(id: Long, direction: Boolean) = IsAuthenticated{user => _ =>
		Exhibition.moveOneStep(id, direction)
		Redirect(routes.Exhibitions.list)
	}

	def moveC(parent:Long, id: Long, direction: Boolean) = IsAuthenticated{user => _ =>
		Exhibition.moveOneStep(id, direction)
		Redirect(routes.Exhibitions.edit(parent))
	}

	def delete(id: Long) = IsAuthenticated{user => _ =>
		Exhibition.delete(id)
		Redirect(routes.Exhibitions.list)
	}

	val fileForm = Form(
		mapping(
			"id"		-> ignored(NotAssigned:Pk[Long])
		)
		(Simple.apply)(Simple.unapply)
	)
	


	/*
	Insert a file
	@arg id: arborescence id
	@arg parent_id: aroborescence parent id
	@arg file_id: {1: main_file, 2: sec file}
	*/
	def insertFile(id: Long, parent_id: Long, file_id: Long) = Action(parse.multipartFormData){implicit request =>
		fileForm.bindFromRequest.fold(
			errors => BadRequest("error"),
			values => {
				request.body.file("file").map { file =>
					import java.io.File

					val contentType = file.contentType
					val timestamp: Long = System.currentTimeMillis / 1000
					
					val resultString = try {

						import models.Utils

						val extension:(Long, String) ={
							contentType.get match{
								case "audio/mp3" 	=> (4,"mp3")
								case "audio/mpeg" => (4,"mp3")
								case "video/mp4" 	=> (5,"mp4")
								case "image/png" 	=> (3,"png")
								case "image/jpeg" => (3,"jpg")
							}
						}

						val filename:String = "main"+id+timestamp+"."+extension._2
				    	file.ref.moveTo(new File(Utils.path+filename), true)

				    	Exhibition.File.insert(id, filename, if(file_id==1){Some(extension._1)}else{None}, file_id)

						"file has been uploaded - "+filename
					} catch {
						case e: Exception => "an error has occurred while uploading the file"+e
					}

					Logger.info(resultString)
					
					
				}
				Redirect(routes.Exhibitions.editSub(id, parent_id))
				.flashing(
    				"success" -> "cat.file.added.successfully"
  				)
			}
		)
	}

	def insertFileEx(id: Long, typ: Long) = Action(parse.multipartFormData){implicit request =>
		fileForm.bindFromRequest.fold(
			errors => BadRequest("error"),
			values => {
				request.body.file("file").map { file =>
					import java.io.File

					val contentType = file.contentType
					val timestamp: Long = System.currentTimeMillis / 1000
					
					val resultString = try {

						import models.Utils

						val extension:(Long, String) ={
							contentType.get match{
								case "image/png" => (3,"png")
							}
						}
						val filename:String = "main"+id+timestamp+"."+extension._2
				    	file.ref.moveTo(new File(Play.application.path+"/public/data/"+filename), true)

				    	Exhibition.File.insert(id, filename, None, typ)

						"file has been uploaded"
					} catch {
						case e: Exception => "an error has occurred while uploading the file"
					}
					
					
				}
				Redirect(routes.Exhibitions.edit(id))
				.flashing(
    				"success" -> "exhibition.file.added.successfully"
  				)
			}
		)
	}

	def deleteFile(id: Long, parent_id: Long, file_nr: Long) = IsAuthenticated{user => _ =>
		Exhibition.File.delete(id, file_nr)
		Redirect(routes.Exhibitions.editSub(id, parent_id))
		.flashing(
    		"success" -> "cat.file.deleted.successfully"
  		)
	}


	
        def upload(id: Long) = Action(parse.temporaryFile) {implicit request =>
           // request.body.moveTo(new File(Play.application.path+"/public/data/picturetest"))
            Ok("File uploaded")
                val file_id = Exhibition.Gallery.insert(id)
                //val mess = Utils.File.upload(request, file_id.get, "gallery")


                val ext:String =".jpg"

                import java.io.File
                val resultString = try {
						val filename:Option[String] = Option[String](Exhibition.Gallery.table+"."+file_id.get+ext)
                        val file = new File(Play.application.path+"/public/data/"+file_id.get+ext)
                        request.body.moveTo(file, true)
			Exhibition.Gallery.insertFileName(file_id.get, filename)


                        "file has been uploaded"
                } catch {
                        case e: Exception => "an error has occurred while uploading the file"
                }

                Ok("{success: true}")
        }



	def deleteGallery(id: Long, cat_id: Long, parent_id: Long) = IsAuthenticated{user => _ =>
		Exhibition.Gallery.delete(id)
		Redirect(routes.Exhibitions.editSub(cat_id, parent_id))
		.flashing(
    		"success" -> "cat.file.deleted.successfully"
  		)
	}

}

object Maps extends Controller with Secured {
  
 
  /* MAPS */

  def upload(id: Long) = Action(parse.temporaryFile) {implicit request =>
      val file_id = id
      val ext:String =".zip"
      val fileName: String = id.toString
      val filePath: String = Play.application.path+"/public/data/maps/"+fileName+"/"
      Logger.info(fileName)
      val resultString = try {
			//val filename:Option[String] = Option[String](Exhibition.Gallery.table+"."+file_id.get+ext)
      val file = new File(filePath+fileName+ext)
      request.body.moveTo(file, true)
    		//Exhibition.Gallery.insertFileName(file_id, filename)
      
      Utils.File.unZip(filePath+fileName+ext, filePath)

      } catch {
      case e: Exception => "an error has occurred while upload-ing the file"
      }
     Ok("{success: true}")
  }

	
def updateMap(id: Long) = Action { request =>
  request.body.asJson.map { json =>
     val shape_id = (json \ "pk").toString().replace("\"", "");
      val shape = json.toString
      val test = Exhibition.Maps.serveSingle(id, shape_id);
      if(test.isEmpty){
         Exhibition.Maps.insert(id, shape_id, shape)
      } else {
         Exhibition.Maps.update(id, shape_id, shape)
      }
      
      
      Ok(json)
  }.getOrElse {
    BadRequest("Expecting Json data")
  }
}

def deleteShape(id: Long) = Action { request =>
  val body: AnyContent = request.body
  val textBody: Option[String] = body.asText 
  
  // Expecting text body
  textBody.map { text =>
      Exhibition.Maps.delete(text)
    Ok("Got: " + text)
  }.getOrElse {
    BadRequest("Expecting text/plain request body")  
  }
  
}



  
  

def annotateMap(id: Long) = Action { request =>
  request.body.asJson.map { json =>
      val shape_id = (json \ "pk").toString().replace("\"", "");
      val annotation = (json \ "value").toString
      
//      var feature  = Json.toJson(Exhibition.Maps.serveSingle(id, shape_id))   
//      val t: JsObject = feature.as[JsObject] ++ Json.obj("annotation" -> annotation)
//      val shape=t.toString
//      Exhibition.Maps.update(id, shape_id, shape)
     
      Exhibition.Maps.annotate(shape_id, annotation)
      Ok(json)
  }.getOrElse {
    BadRequest("Expecting Json data")
  }
}
/*
def serveMaps(id: Long) = Action { request =>
  request.body.asJson.map { json =>
      Exhibition.Maps.serve(id)
      Ok(json)
  }.getOrElse {
    BadRequest("Expecting Json data")
  }
}
*/


  def serveMaps(id: Long) = Action{implicit request => 
  import play.api.libs.json.Json
  

    var p = Exhibition.Maps.serve(id)  
    var shapes = ""
    for (feature <- p) shapes=shapes+feature.toString+", "
      
    var str = "var shapeCollection = {\"type\": \"FeatureCollection\", \"features\": ["+shapes+"]};"

    
    Ok(str)
  }


  def serveAnnotation(id: Long, shape_id: String) = Action{implicit request => 
  import play.api.libs.json.Json
 
    var feature  = Exhibition.Maps.serveSingle(id, shape_id).toString
    //var p = Exhibition.Maps.serveSingle(id, shape_id).toString

    
    Ok(feature)
  }



}


object Galleries extends Controller with Secured{
        def move(id: Long, sid: Long, parent_id: Long, upOrDown: Boolean) = IsAuthenticated{user => _ =>
                Exhibition.Gallery.move(id,sid, upOrDown)
                Redirect(routes.Exhibitions.editSub(sid, parent_id))
        }
		
	def insert(id: Long, parent_id: Long) = IsAuthenticated{user => _ =>
		Exhibition.Gallery.insert(id)

				Redirect(routes.Exhibitions.editSub(id, parent_id))
				.flashing(
    				"success" -> "cat.edited.successfully"
  				)
	}


        def update(id: Long) = IsAuthenticated{user => implicit request =>
                import play.api.libs.json.Json

                myForm.bindFromRequest.fold(
                        errors  => {
                                val r =
                                Json.stringify(Json.obj(
                                        "status" -> "error"
                                ))
                                Ok(r)
                        },
                        data            => {
                                Exhibition.Gallery.update(id, data)
                                val r =
                                Json.stringify(Json.obj(
                                        "status" -> "ok",
                                        "caption" -> data.caption
                                ))
                                Ok(r)
                        }
                )
        }


        val myForm = Form(
                mapping(
                        "caption" -> optional(text)
                )
                (Gallery_e.apply)(Gallery_e.unapply)
        )
}


