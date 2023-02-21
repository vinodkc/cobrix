/*
 * Copyright 2018 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.cobrix.cobol.parser.encoding.codepage

import za.co.absa.cobrix.cobol.internal.Logging

/**
  * A trait for generalizing EBCDIC to ASCII conversion tables for different EBCDIC code pages.
  */
abstract class CodePage extends Serializable {
  /**
    * A short name is used to distinguish between different code pages, so it must be unique.
    */
  def codePageShortName: String

  /**
    * Converts an array of bytes to string according to the rules of the code page.
    */
  def convert(bytes: Array[Byte]): String
}

object CodePage extends Logging {

  def getCodePageByName(codePageName: String): CodePage = {
    codePageName match {
      case "common"          => new CodePageCommon
      case "common_extended" => new CodePageCommonExt
      case "cp037"           => new CodePage037
      case "cp037_extended"  => new CodePage037Ext
      case "cp838"           => new CodePage838
      case "cp870"           => new CodePage870
      case "cp875"           => new CodePage875
      case "cp1025"          => new CodePage1025
      case "cp1047"          => new CodePage1047
      case "cp00300"         => new CodePage00300
      case codePage          => throw new IllegalArgumentException(s"The code page '$codePage' is not one of the builtin EBCDIC code pages.")
    }
  }

  def getCodePageByClass(codePageClass: String): CodePage = {
    logger.info(s"Instantiating code page class: $codePageClass")
    Class.forName(codePageClass,
                  true,
                  Thread.currentThread().getContextClassLoader)
      .newInstance()
      .asInstanceOf[CodePage]
  }
}
