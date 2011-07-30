import scala.xml._;
import scala.util.control.Breaks.{break,breakable};
import com.almworks.sqlite4java._;
import java.io.File;

object Converter {
    class Word {
      var name = "unknown";
      var num = 1;
      var data:List[scala.xml.Node] = List();
    }

    def genWordList(data: Elem) : List[Word] = {
      var entries = (data \\ "entree")

      println("("+entries.length+" entrées à traiter -- '.' = 1000 entrées)")

      var wordlist:List[Word] = Nil
      var itx = 0

      for (entry <- entries) {
        breakable {
          val name = (entry \ "@terme").text

          for (existing_word <- wordlist) {
            if (name == existing_word.name) {
              existing_word.num = existing_word.num + 1
              existing_word.data = (entry :: existing_word.data)
              break
            }
          }

          // Not broken => not found. Just add it in the list.
          val new_word = new Word
          new_word.name = name
          new_word.data = entry :: new_word.data

          wordlist = new_word :: wordlist;

          itx = itx + 1
          if (itx % 100 == 0) print (".");
        }
      }

      println()

      wordlist.sortWith((x:Word,y:Word) => (x.name).<(y.name)) // Lexocagrphical order
    }

    def genHTML_parseText(root: Node) : String = {
      var data = ""
      for (child <- root.child) {
        child.label match {
          case "#PCDATA" => data += child.text.replaceAll("\n", "")

          case "indent" => data += "<span class='indent'>"+genHTML_parseText(child)+"</span>"

          case "variante" =>
            if (child.attribute("num").size > 0) {
              data += "<span class='varnum'>"+child.attribute("num").head.text+"°</span>"
            }
            data += "<span class='variante'>"+genHTML_parseText(child)+"</span>"

          case "rubrique" => data += "<span class='rubrique'"
            if (child.attribute("nom").size > 0) {
              data += " title='"+child.attribute("nom").head.text+"'"
            }
            data += ">"+genHTML_parseText(child)+"</span>"

          case "cit" => data += "<span class='citation'>"
            data += "<span class='cit'>"+genHTML_parseText(child)+"</span>, "
            data += "<span class='aut'>"+child.attribute("aut").head.text+"</span>"
            data += "<span class='ref'>"+child.attribute("ref").head.text+"</span>"
            data += "</span>"

          case "exemple" => data += "<span class='exemple'>"+genHTML_parseText(child)+"</span>"

          // TODO : Implement links !!
          case "a" => data += "<a href='#"+child.attribute("ref").head.text+"'>"+genHTML_parseText(child)+"</a>"

          case "semantique" => data += genHTML_parseText(child)

          case _ => Nil
        }
      }

      data
    }

    def genHTML(entry: Node) : String = {
      var data = ""

      // 1. Print "entete"
      data += "<div class='entete'>"
      if ((entry \ "entete" \ "prononciation" size) > 0)
        data += "<span class='prononciation'>(" + ((entry \ "entete" \ "prononciation").text) + ")</span>"

      if ((entry \ "entete" \ "nature" size) > 0)
        data += "<span class='nature'>"+((entry \ "entete" \ "nature").text)+"</span>"

      data += genHTML_parseText((entry \ "entete").head)

      data += "</div>"

      // 2. Print "resume"
      if ((entry \ "résumé").size > 0) {
        data += "<span class='titrerubrique'>Résumé :</span><span class='resume'>"
        data += genHTML_parseText((entry \ "résumé").head)
        data += "</span>"
      }

      // 3. Print "corps"
      data += genHTML_parseText((entry \ "corps").head)

      // 4. Print "rubrique"
      data += genHTML_parseText(entry)

      data
    }

    def createSchema(db: SQLiteConnection) {
      db.exec("DROP TABLE IF EXISTS `data`");
      db.exec("CREATE VIRTUAL TABLE `data` USING fts3(name VARCHAR(100) NOT NULL, offset INTEGER, size INTEGER);")
    }


    def addIndex(name: String, offset: Int, size: Int, db: SQLiteConnection) {
      var st = db.prepare("INSERT INTO `data` (`name`, `offset`, `size`) VALUES (?, ?, ?)")

      st.bind(1, name)
      st.bind(2, offset)
      st.bind(3, size)

      st.step()
      st.dispose()
    }

	def main(args: Array[String]) {
      println("Chargement du fichier XML ...")
      var data = XML.loadFile("xmlittre.xml")

      println("Ouverture de la base SQLite ...")
      var db = new SQLiteConnection(new File("XMLittre.idx"))
      db.open(true)
      createSchema(db)

      println("Creation de la liste des entrees ...")
      var wordlist = genWordList(data)

      println("Generation de l'index ("+(wordlist.size)+" mots) ...")

      var itx = 0
      var offset = 0

      var fstream = new java.io.FileWriter("XMLittre.db")
      var out = new java.io.BufferedWriter(fstream)

      db.exec("BEGIN TRANSACTION");

      for (word <- wordlist) {
        for (entry <- word.data) {
          val html = genHTML(entry)
          val htmllength = html.getBytes("UTF-8").length
          var name = word.name

          if (word.num > 1) {
            name = name+" "+((entry \ "@sens").text)+"."
          }

          addIndex(name, offset, htmllength, db)
          offset = offset+htmllength

          out.write(html)
        }
        itx = itx+1
        if (itx % 100 == 0) print(".")
    }
    println()
    println("offset="+offset)

    db.exec("COMMIT TRANSACTION");
    out.close()
    db.dispose()
  }
}
