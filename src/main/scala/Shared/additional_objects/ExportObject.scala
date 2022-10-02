package nl.rug.aoop
package Shared.additional_objects

/**
 * The ExportObject is the main Object that is send between Server & Client
 *
 * @param objectType The Type of the Object that is hidden in the data
 * @param data       The data as a String
 */
case class ExportObject(objectType: Class[_], data: String) extends Serializable
