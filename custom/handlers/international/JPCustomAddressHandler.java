package net.intermedia.uav.postal.custom.handlers.international;

import net.intermedia.uav.country.Country;
import net.intermedia.uav.country.CountryState;
import net.intermedia.uav.pojo.postal.PostalValidationOutputLanguage;
import net.intermedia.uav.postal.UavPostalValidationResult;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

public class JPCustomAddressHandler extends InternationalAbstractCustomAddressHandler {

    @Override
    public String generateAddressLine1(UavPostalValidationResult uavPostalValidationResult) {
        return uavPostalValidationResult.getAddressLine1();
    }

    @Override
    public String generateAddressLine2(UavPostalValidationResult uavPostalValidationResult) {
        return uavPostalValidationResult.getAddressLine2();
    }

    @Override
    public void fillAddressLine1InUavPostalValidationResult(UavPostalValidationResult uavPostalValidationResult) {
        //do nothing, because addressLine1 for JP is already filled  in CustomAddressRequestToUavPostalValidationResultMapper
    }

    @Override
    public void fillAddressLine2InUavPostalValidationResult(UavPostalValidationResult uavPostalValidationResult) {
        //do nothing, because addressLine2 for JP is already filled  in CustomAddressRequestToUavPostalValidationResultMapper
    }

    @Override
    public String[] generateFullAddress(UavPostalValidationResult uavPostalValidationResult) {
        String state = uavPostalValidationResult.getState();
        if (StringUtils.isNumeric(state)) {
            state = "JP-" + state;
        }
        CountryState countryState = CountryState.find(Country.JP, state);
        return Stream.of(uavPostalValidationResult.getAddressLine1(), uavPostalValidationResult.getAddressLine2(), uavPostalValidationResult.getAddressLine3(),
                String.join(" ", uavPostalValidationResult.getCity(),
                        (countryState.isFound() ? countryState.getShortName() : uavPostalValidationResult.getState()),
                        uavPostalValidationResult.getZip()).trim())
                .filter(add -> !StringUtils.isEmpty(add))
                .toArray(String[]::new);
    }

    @Override
    public void fillStateName(UavPostalValidationResult uavPostalValidationResult) {
        PostalValidationOutputLanguage outputLanguage = uavPostalValidationResult.getPostalValidationOutputLanguage();
        String state = uavPostalValidationResult.getState();
        if (StringUtils.isNumeric(state)) {
            state = "JP-" + state;
        }
        CountryState countryState = CountryState.find(uavPostalValidationResult.getCountry(), state);
        if (countryState.isFound()) {
            uavPostalValidationResult.setState(countryState.getUavState());
            if (outputLanguage == PostalValidationOutputLanguage.ENGLISH) {
                uavPostalValidationResult.setStateName(countryState.getStateName());
            } else {
                uavPostalValidationResult.setStateName(countryState.getLocalName());
            }
        }
    }
}
