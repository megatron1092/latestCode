package net.intermedia.uav.postal;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.log4j.Log4j2;
import net.intermedia.uav.Mapping;
import net.intermedia.uav.cache.UavPostalCachedObject;
import net.intermedia.uav.context.ValidationContext;
import net.intermedia.uav.context.ValidationContextHolder;
import net.intermedia.uav.country.Country;
import net.intermedia.uav.pojo.UserView;
import net.intermedia.uav.pojo.postal.PostalValidationOutputLanguage;
import net.intermedia.uav.pojo.postal.PostalValidationRequest;
import net.intermedia.uav.pojo.postal.PostalValidationResult;
import net.intermedia.uav.pojo.postal.SearchAddressType;
import net.intermedia.uav.pojo.postal.SearchCustomAddressesResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * REST controller that handle postal address validation requests
 */
@Api
@Log4j2
@RestController
@RequestMapping(Mapping.POSTAL)
public class PostalValidationRestController {

    @Autowired
    private PostalValidationService postalValidationService;

    /**
     * Validates postal address. Additionally performs address validation for tax if <code>checkTax</code> is set to true
     * and emergency if <code>checkE911</code> is set to true.
     */
    @ApiOperation(value = "Validates postal address.",
            notes = "Additionally performs address validation for tax if checkTax is set to true\n" +
                    " and emergency if checkE911 is set to true.")
    @GetMapping(Mapping.POSTAL_VALIDATE)
    @JsonView(UserView.class)
    public PostalValidationResult validate(@Valid PostalValidationRequest postalValidationRequest) throws MissingServletRequestParameterException {
        checkRequest(postalValidationRequest);
        ValidationContext validationContext = ValidationContextHolder.get();

        UavPostalValidationResult result = postalValidationService.validate(validationContext, postalValidationRequest);

        if (validationContext != null) {
            result.setUavRequestID(validationContext.getRequestId().toString());
            result.setClientRequestID(validationContext.getClientRequestID());
        }

        log.info("responseLog: " + result);

        return result;
    }

    @ApiOperation(
            value = "Lookup address in cache.",
            notes = "Lookup address in cache. If address is not found it will be validated and cached object will be returned."
    )
    @GetMapping(Mapping.LOOKUP_ADDRESS)
    public UavPostalCachedObject lookup(@Valid PostalValidationRequest postalValidationRequest) throws MissingServletRequestParameterException {
        checkRequest(postalValidationRequest);
        return postalValidationService.lookup(ValidationContextHolder.get(), postalValidationRequest);
    }

    @ApiOperation(
            value = "Lookup address by uid in cache",
            notes = "Lookup address by uid in cache. If address is not found empty cached object will be returned"
    )
    @GetMapping(value = Mapping.LOOKUP_ADDRESS + "/{uid}")
    public UavPostalCachedObject lookupByUid(@PathVariable("uid") String uid) {
        return postalValidationService.lookupByUid(uid);
    }

    @ApiOperation(
            value = "Search custom addresses",
            notes = "Search custom addresses by fields: country, state, city, zip, full_street_address"
    )
    @GetMapping(value = Mapping.SEARCH)
    public SearchCustomAddressesResponse searchCustom(@RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                      @RequestParam(value = "pageNumber", required = false) Integer pageNumber,
                                                      @RequestParam(value = "searchAddressType", required = false, defaultValue = "ALL") SearchAddressType searchAddressType,
                                                      @RequestParam(value = "country", required = false) String country,
                                                      @RequestParam(value = "state", required = false) String state,
                                                      @RequestParam(value = "city", required = false) String city,
                                                      @RequestParam(value = "zip", required = false) String zip,
                                                      @RequestParam(value = "full_street_address", required = false) String fullStreetAddress) {
        if (pageNumber != null && pageNumber < 1) {
            throw new IllegalStateException("pageNumber must be equals or more than 1");
        } else if (pageNumber != null) {
            pageNumber = pageNumber - 1;
        }
        if (pageSize != null && pageSize < 1) {
            throw new IllegalStateException("pageSize must be equals or more than 1");
        }
        SearchCustomAddressesResponse response = postalValidationService.searchCustomByParams(pageSize, pageNumber, searchAddressType, country, state, city, zip, fullStreetAddress);
        response.setReturnedPageNumber(response.getReturnedPageNumber()+1);
        return response;
    }

    private boolean isOutputLanguageRequired(String c) {
        Country country = Country.find(c);
        return !(country == Country.US
                || country == Country.CA
                || country == Country.PR);

    }

    private void checkRequest(PostalValidationRequest postalValidationRequest) throws MissingServletRequestParameterException {
        if (StringUtils.isEmpty(postalValidationRequest.getZip())
                && (StringUtils.isEmpty(postalValidationRequest.getCity()) || StringUtils.isEmpty(postalValidationRequest.getState()))) {
            throw new MissingServletRequestParameterException("zip or city and state", "String");
        }

        if (postalValidationRequest.getOutputLanguage() == null && isOutputLanguageRequired(postalValidationRequest.getCountry())) {
            postalValidationRequest.setOutputLanguage(PostalValidationOutputLanguage.LOCAL_ROMAN);
        }
    }


}
