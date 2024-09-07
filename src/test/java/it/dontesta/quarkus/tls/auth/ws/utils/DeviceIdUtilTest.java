/*
 * Copyright (c) 2024 Antonio Musarra's Blog.
 * SPDX-License-Identifier: MIT
 */

package it.dontesta.quarkus.tls.auth.ws.utils;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DeviceIdUtilTest {

  @Test
  void generateDeviceIdReturnsValidId() {
    String deviceId = DeviceIdUtil.generateDeviceId();
    assertNotNull(deviceId);
    assertFalse(deviceId.isEmpty());
  }

  @Test
  void generateDeviceIdIsUnique() {
    String deviceId1 = DeviceIdUtil.generateDeviceId();
    String deviceId2 = DeviceIdUtil.generateDeviceId();
    assertNotEquals(deviceId1, deviceId2);
  }

  @Test
  void validateDeviceIdReturnsTrueForValidId() {
    String validDeviceId = DeviceIdUtil.generateDeviceId();
    assertTrue(DeviceIdUtil.verifyDeviceId(validDeviceId));
  }

  @Test
  void validateDeviceIdReturnsFalseForInvalidId() {
    String invalidDeviceId = "invalid-id";
    assertFalse(DeviceIdUtil.verifyDeviceId(invalidDeviceId));
  }

  @Test
  void validateDeviceIdReturnsFalseForNullId() {
    assertFalse(DeviceIdUtil.verifyDeviceId(null));
  }

  @Test
  void validateDeviceIdReturnsFalseForEmptyId() {
    assertFalse(DeviceIdUtil.verifyDeviceId(""));
  }

  @Test
  void validateDeviceIdOk() {
    String deviceIdToValidate = "MTcyNTcxMjkwNzU4MDIzMjAwMCMyZjU1ZjZiYS05OTZmLTRjZTctYWQzNC0zNWRkYTNmOTU3YTkjYW11c2FycmEtbWFjYm9vay1wcm8ubG9jYWwjMjMwODA2Y2QwMGEzNTBkODZhMjUzNGViYTcyMmUyY2JmOGJlMmM5NTdmNmU5NTRmMjg1Yjc5MTFjNDY0ZDdiMw==";
    assertTrue(DeviceIdUtil.verifyDeviceId(deviceIdToValidate));
  }
}