package net.intermedia.uav.postal.custom;

import net.intermedia.uav.country.Country;
import net.intermedia.uav.country.CountryState;
import net.intermedia.uav.pojo.postal.CustomAddressRequest;
import net.intermedia.uav.pojo.postal.PostalValidationOutputLanguage;
import net.intermedia.uav.postal.UavPostalValidationResult;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import static net.intermedia.uav.pojo.postal.PostalValidationResult.Result.AV24;

@Mapper(componentModel = "spring")
public abstract class CustomAddressRequestToUavPostalValidationResultMapper {
    @Mapping(source = "country", target = "country", qualifiedByName = "normalizedString")
    @Mapping(source = "houseNumber", target = "houseNumber", qualifiedByName = "normalizedString")
    @Mapping(source = "preDirectional", target = "preDir", qualifiedByName = "normalizedString")
    @Mapping(source = "streetName", target = "streetName", qualifiedByName = "normalizedString")
    @Mapping(source = "streetSuffix", target = "suffix", qualifiedByName = "normalizedString")
    @Mapping(source = "postDirectional", target = "postDir", qualifiedByName = "normalizedString")
    @Mapping(source = "unitType", target = "unitType", qualifiedByName = "normalizedString")
    @Mapping(source = "unitNumber", target = "unitNumber", qualifiedByName = "normalizedString")
    @Mapping(source = "request", target = "city", qualifiedByName = "fillCity")
    @Mapping(source = "request", target = "state", qualifiedByName = "fillState")
    @Mapping(source = "zip", target = "zip", qualifiedByName = "normalizedString")
    @Mapping(source = "request", target = "isPostBox", qualifiedByName = "checkIsPostBoxWithPmb")
    @Mapping(source = "shippingValid", target = "shippingValid", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "postalValid", target = "postalValid", defaultValue = "true")
    @Mapping(source = "taxValid", target = "taxValid", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "outputLanguage", target = "outputLanguage", defaultValue = "ENGLISH")
    @Mapping(source = "request", target = "pmbPrefix", qualifiedByName = "checkForPmbPrefix")
    @Mapping(source = "request", target = "pmbNumber", qualifiedByName = "checkForPmbNumber")
    @Mapping(source = "poBoxNumber", target = "postBox", qualifiedByName = "normalizedString")
    @Mapping(source = "request", target = "addressLine1", qualifiedByName = "fillJPAddressLine1")
    @Mapping(source = "request", target = "addressLine2", qualifiedByName = "fillJPAddressLine2")
    public abstract UavPostalValidationResult mapCustomAddressRequestToUavPostalValidationResult(CustomAddressRequest request);

    @AfterMapping
    protected void addResultCodes(@MappingTarget UavPostalValidationResult.Builder builder){
        builder.addResult(AV24);
    }

    @Named("checkForPmbPrefix")
    static String checkCountryForPmbPrefix(CustomAddressRequest request) {
        return checkCountryForParameter(request.getCountry(), request.getPmbPrefix());
    }

    @Named("checkForPmbNumber")
    static String checkCountryForPmbNumber(CustomAddressRequest request) {
        return checkCountryForParameter(request.getCountry(), request.getPmbNumber());
    }

    @Named("checkIsPostBoxWithPmb")
    static Boolean checkIsPostBoxWithPmb(CustomAddressRequest request) {
        String country = request.getCountry();
        Country foundCountry = Country.find(country);
        if (foundCountry != Country.US && foundCountry != Country.PR &&
                (StringUtils.isNotBlank(request.getPmbPrefix()) || StringUtils.isNotBlank(request.getPmbNumber()))
                && BooleanUtils.isTrue(request.getIsPostBox())) {
            return false;
        }
        return request.getIsPostBox();
    }

    @Named("fillJPAddressLine1")
    static String fillJPAddressLine1(CustomAddressRequest request) {
        String country = request.getCountry();
        Country foundCountry = Country.find(country);
        if (foundCountry == Country.JP) {
            return normalize(request.getAddressLine1());
        }
        return "";
    }

    @Named("fillJPAddressLine2")
    static String fillJPAddressLine2(CustomAddressRequest request) {
        String country = request.getCountry();
        Country foundCountry = Country.find(country);
        if (foundCountry == Country.JP) {
            return normalize(request.getAddressLine2());
        }
        return "";
    }

    @Named("fillCity")
    static String fillCity(CustomAddressRequest request) {
        String country = request.getCountry();
        Country foundCountry = Country.find(country);
        if (!(foundCountry == Country.JP && request.getOutputLanguage() != PostalValidationOutputLanguage.ENGLISH)) {
            return normalize(request.getCity());
        }
        return "";
    }

    @Named("fillState")
    static String fillState(CustomAddressRequest request) {
        String country = request.getCountry();
        Country foundCountry = Country.find(country);
        String state = request.getState();
        CountryState countryState = CountryState.find(foundCountry, state);
        if (countryState.isFound()){
            return countryState.getStateCode();
        } else return state;
    }


    static String checkCountryForParameter(String country, String parameter) {
        Country foundCountry = Country.find(country);
        if (foundCountry == Country.US || foundCountry == Country.PR) {
            return normalize(parameter);
        } else return "";
    }

    @Named("normalizedString")
    static String normalizedString(String sourceString) {
        return normalize(sourceString);
    }

    static String normalize(String sourceString) {
        if (sourceString == null) return null;
        return sourceString.replaceAll("\\t", " ").replaceAll("\\s{2,}", " ").trim();
    }


}
