/*
 * Copyright 2009 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.pdf417.decoder;

import com.google.zxing.FormatException;
import com.google.zxing.pdf417.PDF417ResultMetadata;
import com.google.zxing.common.DecoderResult;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link DecodedBitStreamParser}.
 */
public class PDF417DecoderTestCase extends Assert {

  /**
   * Tests the first sample given in ISO/IEC 15438:2015(E) - Annex H.4
   */
  @Test
  public void testStandardSample1() throws FormatException {
    PDF417ResultMetadata resultMetadata = new PDF417ResultMetadata();
    int[] sampleCodes = {20, 928, 111, 100, 17, 53, 923, 1, 111, 104, 923, 3, 64, 416, 34, 923, 4, 258, 446, 67,
      // we should never reach these
      1000, 1000, 1000};

    DecodedBitStreamParser.decodeMacroBlock(sampleCodes, 2, resultMetadata);

    assertEquals(0, resultMetadata.getSegmentIndex());
    assertEquals("017053", resultMetadata.getFileId());
    assertFalse(resultMetadata.isLastSegment());
    assertEquals(4, resultMetadata.getSegmentCount());
    assertEquals("CEN BE", resultMetadata.getSender());
    assertEquals("ISO CH", resultMetadata.getAddressee());

    @SuppressWarnings("deprecation")
    int[] optionalData = resultMetadata.getOptionalData();
    assertEquals("first element of optional array should be the first field identifier", 1, optionalData[0]);
    assertEquals("last element of optional array should be the last codeword of the last field",
        67, optionalData[optionalData.length - 1]);
  }


  /**
   * Tests the second given in ISO/IEC 15438:2015(E) - Annex H.4
   */
  @Test
  public void testStandardSample2() throws FormatException {
    PDF417ResultMetadata resultMetadata = new PDF417ResultMetadata();
    int[] sampleCodes = {11, 928, 111, 103, 17, 53, 923, 1, 111, 104, 922,
      // we should never reach these
      1000, 1000, 1000};

    DecodedBitStreamParser.decodeMacroBlock(sampleCodes, 2, resultMetadata);

    assertEquals(3, resultMetadata.getSegmentIndex());
    assertEquals("017053", resultMetadata.getFileId());
    assertTrue(resultMetadata.isLastSegment());
    assertEquals(4, resultMetadata.getSegmentCount());
    assertNull(resultMetadata.getAddressee());
    assertNull(resultMetadata.getSender());

    @SuppressWarnings("deprecation")
    int[] optionalData = resultMetadata.getOptionalData();
    assertEquals("first element of optional array should be the first field identifier", 1, optionalData[0]);
    assertEquals("last element of optional array should be the last codeword of the last field",
        104, optionalData[optionalData.length - 1]);
  }


  /**
   * Tests the example given in ISO/IEC 15438:2015(E) - Annex H.6
   */
  @Test
  public void testStandardSample3() throws FormatException {
    PDF417ResultMetadata resultMetadata = new PDF417ResultMetadata();
    int[] sampleCodes = {7, 928, 111, 100, 100, 200, 300,
      0}; // Final dummy ECC codeword required to avoid ArrayIndexOutOfBounds

    DecodedBitStreamParser.decodeMacroBlock(sampleCodes, 2, resultMetadata);

    assertEquals(0, resultMetadata.getSegmentIndex());
    assertEquals("100200300", resultMetadata.getFileId());
    assertFalse(resultMetadata.isLastSegment());
    assertEquals(-1, resultMetadata.getSegmentCount());
    assertNull(resultMetadata.getAddressee());
    assertNull(resultMetadata.getSender());
    assertNull(resultMetadata.getOptionalData());

    // Check that symbol containing no data except Macro is accepted (see note in Annex H.2)
    DecoderResult decoderResult = DecodedBitStreamParser.decode(sampleCodes, "0");
    assertEquals("", decoderResult.getText());
    assertNotNull(decoderResult.getOther());
  }

  @Test
  public void testSampleWithFilename() throws FormatException {
    int[] sampleCodes = {23, 477, 928, 111, 100, 0, 252, 21, 86, 923, 0, 815, 251, 133, 12, 148, 537, 593,
        599, 923, 1, 111, 102, 98, 311, 355, 522, 920, 779, 40, 628, 33, 749, 267, 506, 213, 928, 465, 248,
        493, 72, 780, 699, 780, 493, 755, 84, 198, 628, 368, 156, 198, 809, 19, 113};
    PDF417ResultMetadata resultMetadata = new PDF417ResultMetadata();

    DecodedBitStreamParser.decodeMacroBlock(sampleCodes, 3, resultMetadata);

    assertEquals(0, resultMetadata.getSegmentIndex());
    assertEquals("000252021086", resultMetadata.getFileId());
    assertFalse(resultMetadata.isLastSegment());
    assertEquals(2, resultMetadata.getSegmentCount());
    assertNull(resultMetadata.getAddressee());
    assertNull(resultMetadata.getSender());
    assertEquals("filename.txt", resultMetadata.getFileName());
  }

  @Test
  public void testSampleWithNumericValues() throws FormatException {
    int[] sampleCodes = {25, 477, 928, 111, 100, 0, 252, 21, 86, 923, 2, 2, 0, 1, 0, 0, 0, 923, 5, 130, 923,
        6, 1, 500, 13, 0};
    PDF417ResultMetadata resultMetadata = new PDF417ResultMetadata();

    DecodedBitStreamParser.decodeMacroBlock(sampleCodes, 3, resultMetadata);

    assertEquals(0, resultMetadata.getSegmentIndex());
    assertEquals("000252021086", resultMetadata.getFileId());
    assertFalse(resultMetadata.isLastSegment());

    assertEquals(180980729000000L, resultMetadata.getTimestamp());
    assertEquals(30, resultMetadata.getFileSize());
    assertEquals(260013, resultMetadata.getChecksum());
  }

  @Test
  public void testSampleWithMacroTerminatorOnly() throws FormatException {
    int[] sampleCodes = {7, 477, 928, 222, 198, 0, 922};
    PDF417ResultMetadata resultMetadata = new PDF417ResultMetadata();

    DecodedBitStreamParser.decodeMacroBlock(sampleCodes, 3, resultMetadata);

    assertEquals(99998, resultMetadata.getSegmentIndex());
    assertEquals("000", resultMetadata.getFileId());
    assertTrue(resultMetadata.isLastSegment());
    assertEquals(-1, resultMetadata.getSegmentCount());
    assertNull(resultMetadata.getOptionalData());
  }

  @Test(expected = FormatException.class)
  public void testSampleWithBadSequenceIndexMacro() throws FormatException {
    int[] sampleCodes = {3, 928, 222, 0};
    PDF417ResultMetadata resultMetadata = new PDF417ResultMetadata();
    DecodedBitStreamParser.decodeMacroBlock(sampleCodes, 2, resultMetadata);
  }

  @Test(expected = FormatException.class)
  public void testSampleWithNoFileIdMacro() throws FormatException {
    int[] sampleCodes = {4, 928, 222, 198, 0};
    PDF417ResultMetadata resultMetadata = new PDF417ResultMetadata();
    DecodedBitStreamParser.decodeMacroBlock(sampleCodes, 2, resultMetadata);
  }

  @Test(expected = FormatException.class)
  public void testSampleWithNoDataNoMacro() throws FormatException {
    int[] sampleCodes = {3, 899, 899, 0};
    DecodedBitStreamParser.decode(sampleCodes, "0");
  }

}
