package nl.rug.aoop
package Global

import Shared.additional_objects.ExportObject

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.util.Base64
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import scala.util.{Try, Using}

/**
 * The De/Serializer
 */
object Serializer {

  /**
   * Serialize a List of ExportObjects to a String
   *
   * @param inputs A list of input objects
   * @return A Try String, if successful the list as a Base64 encoded compressed string
   */
  def serialize(inputs: List[ExportObject]): Try[String] = {
    Using.Manager { use =>
      val stream = use(new ByteArrayOutputStream())
      val compressStream = use(new GZIPOutputStream(stream))
      val oStream = new ObjectOutputStream(compressStream)

      oStream.writeObject(inputs)
      oStream.close()

      new String(Base64.getEncoder.encode(stream.toByteArray))
    }
  }


  /**
   * Deserialize a Base64 encoded compressed String to a list of ExportObjects
   *
   * @param inputs The input string
   * @return A Try of a List of Export Objects
   */
  def deSerialize(serialized: String): Try[List[ExportObject]] = {
    Using.Manager { use =>
      val stream = use(new ByteArrayInputStream(Base64.getDecoder.decode(serialized)))
      val decompressStream = use(new GZIPInputStream(stream))
      val oStream = use(new ObjectInputStream(decompressStream))

      oStream.readObject().asInstanceOf[List[ExportObject]]
    }
  }
}
