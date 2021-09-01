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

package za.co.absa.cobrix.cobol.reader.extractors.raw

import org.scalatest.WordSpec
import za.co.absa.cobrix.cobol.parser.CopybookParser
import za.co.absa.cobrix.cobol.reader.memorystream.TestByteStream

class VariableBlockVariableRecordExtractorSuite extends WordSpec {
  private val copybookContent =
    """      01 RECORD.
          02 X PIC X(1).
    """
  private val copybook = CopybookParser.parseTree(copybookContent)

  "little-endian block-included record-included case" should {
    "be able to read a VBVR file that has one record per block" in {
      val rc = getRawRecordContext(1, true, true)

      val extractor = new VariableBlockVariableRecordExtractor(rc)

      assert(extractor.hasNext)

      val r0 = extractor.next()
      assert(r0.length == 1)
      assert(r0.head == 0xF0.toByte)

      assert(extractor.next().head == 0xF1.toByte)
      assert(extractor.next().head == 0xF2.toByte)
      assert(!extractor.hasNext)
    }

    "be able to read a VBVR file that has multiple records per block" in {
      val rc = getRawRecordContext(3, true, true)

      val extractor = new VariableBlockVariableRecordExtractor(rc)

      assert(extractor.hasNext)

      val r0 = extractor.next()
      assert(r0.length == 1)
      assert(r0.head == 0xF0.toByte)

      assert(extractor.next().head == 0xF1.toByte)
      assert(extractor.next().head == 0xF2.toByte)
      assert(extractor.next().head == 0xF3.toByte)
      assert(extractor.next().head == 0xF4.toByte)
      assert(extractor.next().head == 0xF5.toByte)
      assert(extractor.next().head == 0xF6.toByte)
      assert(extractor.next().head == 0xF7.toByte)
      assert(extractor.next().head == 0xF8.toByte)
      assert(!extractor.hasNext)
    }
  }

  "failures" should {
    "throw an exception if a block header is too small" in {
      val rc = getRawRecordContext(Array[Byte](0, 22))

      val extractor = new VariableBlockVariableRecordExtractor(rc)

      val ex = intercept[NumberFormatException] {
        extractor.hasNext
      }

      assert(ex.getMessage.contains("Zero length BigInteger"))
    }

    "throw an exception if a record header is too small" in {
      val rc = getRawRecordContext(Array[Byte](0, 3, 0, 0, 0, 1, 0, 0))

      val extractor = new VariableBlockVariableRecordExtractor(rc)

      val ex = intercept[NoSuchElementException] {
        extractor.next()
      }

      assert(ex.getMessage.contains("queue empty"))
    }
  }

  private def getRawRecordContext(recordsPerBlock: Int,
                                  includeBdwInHeaderSize: Boolean,
                                  includeRdwInHeaderSize: Boolean): RawRecordContext = {
    val numOfBlocks = 3

    val bdwHeaderAdjust = if (includeBdwInHeaderSize) 4 else 0
    val rdwHeaderAdjust = if (includeRdwInHeaderSize) 4 else 0

    val bytes = Range(0, numOfBlocks)
      .flatMap(i => {
        Array[Byte](0, (recordsPerBlock * 5 + bdwHeaderAdjust).toByte, 0, 0) ++ Range(0, recordsPerBlock).flatMap(j => {
          val num = (i * recordsPerBlock + j) % 10
          Array[Byte](0, (rdwHeaderAdjust + 1).toByte, 0, 0, (0xF0 + num).toByte)
        })
      }).toArray[Byte]

    getRawRecordContext(bytes)
  }

  private def getRawRecordContext(bytes: Array[Byte]): RawRecordContext = {
    val ibs = new TestByteStream(bytes)

    RawRecordContext(0, ibs, copybook, "")
  }

}
