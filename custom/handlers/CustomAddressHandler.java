package net.intermedia.uav.postal.custom.handlers;

import net.intermedia.uav.postal.UavPostalValidationResult;

public interface CustomAddressHandler {
    void fillAddressLine1InUavPostalValidationResult(UavPostalValidationResult uavPostalValidationResult);
    void fillAddressLine2InUavPostalValidationResult(UavPostalValidationResult uavPostalValidationResult);
    void checkAddressLines1and2(UavPostalValidationResult uavPostalValidationResult);
    void populateOtherFields(UavPostalValidationResult uavPostalValidationResult);
    void fillPostBoxField(UavPostalValidationResult uavPostalValidationResult);
    void fillTimezone(UavPostalValidationResult uavPostalValidationResult);
    void fillCustomAddressRelatedFields(UavPostalValidationResult uavPostalValidationResult);
    void fillStateName(UavPostalValidationResult uavPostalValidationResult);
    void fillFullAddress(UavPostalValidationResult uavPostalValidationResult);
}
