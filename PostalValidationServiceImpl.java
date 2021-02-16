package net.intermedia.uav.postal;

import lombok.extern.log4j.Log4j2;
import net.intermedia.uav.cache.UavCacheService;
import net.intermedia.uav.cache.UavPostalCachedObject;
import net.intermedia.uav.common.utils.async.UavAsyncExecutor;
import net.intermedia.uav.common.utils.log.LoggingUtils;
import net.intermedia.uav.context.ValidationContext;
import net.intermedia.uav.dataprovider.PostalValidationException;
import net.intermedia.uav.dataprovider.ProviderException;
import net.intermedia.uav.pojo.ValidationError;
import net.intermedia.uav.pojo.postal.E911RC;
import net.intermedia.uav.pojo.postal.PostalRC;
import net.intermedia.uav.pojo.postal.PostalRequest;
import net.intermedia.uav.pojo.postal.PostalValidationRequest;
import net.intermedia.uav.pojo.postal.PostalValidationResult;
import net.intermedia.uav.pojo.postal.SearchAddressType;
import net.intermedia.uav.pojo.postal.SearchCustomAddressesResponse;
import net.intermedia.uav.pojo.postal.ShippingRC;
import net.intermedia.uav.pojo.postal.TaxRC;
import net.intermedia.uav.postal.e911.E911ValidationService;
import net.intermedia.uav.postal.shipping.ShipValidationService;
import net.intermedia.uav.postal.tax.TaxValidationService;
import net.intermedia.uav.utils.AlarmService;
import net.intermedia.uav.utils.UavCorrectionsService;
import net.intermedia.uav.utils.ValidationUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static net.intermedia.uav.pojo.ErrorCode.EX02;
import static net.intermedia.uav.pojo.ErrorCode.EX03;
import static net.intermedia.uav.pojo.ErrorCode.EX05;
import static net.intermedia.uav.pojo.ErrorCode.EX06;
import static net.intermedia.uav.pojo.ErrorCode.VE05;
import static net.intermedia.uav.pojo.ErrorCode.VE06;
import static net.intermedia.uav.pojo.ErrorCode.VE12;
import static net.intermedia.uav.pojo.ErrorCode.VE14;

/**
 * Business logic for postal validation. Different validation types are performed in parallel where this is possible.
 */
@Log4j2
@Service
public class PostalValidationServiceImpl implements PostalValidationService {

    protected final AlarmService alarmService;
    private final MailValidationService mailValidationService;
    private final TaxValidationService taxValidationService;
    private final E911ValidationService e911ValidationService;
    private final ShipValidationService shipValidationService;

    @Autowired
    private UavCorrectionsService uavCorrectionsService;

    @Autowired
    private UavCacheService uavCacheService;

    @Autowired
    private UavAsyncExecutor uavAsyncExecutor;

    private final AuthenticationTrustResolver authenticationTrustResolver;

    @Autowired
    public PostalValidationServiceImpl(MailValidationService mailValidationService,
                                       TaxValidationService taxValidationService,
                                       E911ValidationService e911ValidationService,
                                       ShipValidationService shipValidationService,
                                       AlarmService alarmService) {
        this.mailValidationService = mailValidationService;
        this.taxValidationService = taxValidationService;
        this.e911ValidationService = e911ValidationService;
        this.shipValidationService = shipValidationService;
        this.alarmService = alarmService;
        this.authenticationTrustResolver = new AuthenticationTrustResolverImpl();
    }

    public <T extends PostalRequest> UavPostalValidationResult validate(ValidationContext validationContext, T postalRequest) {

        UavPostalCachedObject uavPostalCachedObject = uavCacheService.getUavPostalCachedObject(postalRequest);

        UavPostalValidationResult result = uavPostalCachedObject.getPostalValidationResult();
        Boolean checkE911 = postalRequest.getCheckE911();
        Boolean checkTax = postalRequest.getCheckTax();
        Boolean checkShip = postalRequest.getCheckShip();
        boolean updateInCacheAfterRevalidate = ((BooleanUtils.isTrue(checkE911) && !uavPostalCachedObject.hasE911Validation())
                || (BooleanUtils.isTrue(checkTax) && !uavPostalCachedObject.hasTaxValidation())
                || (BooleanUtils.isTrue(checkShip) && !uavPostalCachedObject.hasShippingValidation()));
        boolean isAlreadyExistInCache = false;
        if (result == null) {
            // first of all lets get an mail validation result, as this is necessary for any type of validation
            result = mailValidationService.validateAddress(validationContext, uavPostalCachedObject, postalRequest);
        } else {
            isAlreadyExistInCache = true;

            /*
             * SPBVBO-3604: created for related bug to avoid problem on production.
             * Should be deleted after problem solving.
             */
            uavCorrectionsService.actualizeTaxValidation(uavPostalCachedObject);
            /*
            SPBVBO-3633 we need to return actual corrections, not saved ones,
            because different non-normalized keys can lead to same normalized entry in cache.
            In this case corrections could be confusing for a client-side.
            */
            uavCorrectionsService.checkTypos(postalRequest.getAddressLine1(), postalRequest.getAddressLine2(),
                    postalRequest.getAddressLine3(), postalRequest.getCity(), postalRequest.getZip(),
                    postalRequest.getState(), result.getAddressLine1(), result.getAddressLine2(), result.getAddressLine3(),
                    result.getCity(), result.getZip(), result.getState(), result);
            uavCorrectionsService.invalidateDuplicateCorrections(result);
        }

        // make sure, we have a proper mailing validation result
        if (result == null || result.getPostalRC() == null || result.getPostalRC().getPostalValid() == null || result.getSyntaxValid() == null) {
            throw new PostalValidationException("Mailing validation result should not be null and " +
                    "both isPostalValid and syntaxValid should be defined. However, we got the following mailing validation result: " + result);
        }

        result.setChanged(anyCorrectionsMade(result.getPostalRC()));

        // check if we cannot continue the validation
        if (containsFatalError(result.getPostalRC())) {
            return result;
        }

        result.setPostalValidationOutputLanguage(postalRequest.getOutputLanguage());

        UavPostalValidationResult finalResult = result;

        CompletableFuture<UavPostalValidationResult> completableFuture = CompletableFuture.completedFuture(finalResult);

        if (BooleanUtils.isTrue(checkShip)) {
            completableFuture = executeAsync(() -> shipValidationService.validate(finalResult, uavPostalCachedObject),
                    validationContext, completableFuture,
                    (res, error) -> {
                        ShippingRC rc = finalResult.getShippingRC();
                        if (rc == null) {
                            rc = new ShippingRC();
                        }

                        error.setErrorCode(EX05);
                        rc.addError(error);
                        rc.setShippingValid(false);
                        res.setShippingRC(rc);
                    });
        }

        if (BooleanUtils.isTrue(checkTax)) {
            // Only if the addresses syntactically correct tax validation is done.
            BiConsumer<UavPostalValidationResult, ValidationError> taxErrorSetter = (res, error) -> {
                TaxRC rc = res.getTaxRC();
                if (rc == null) {
                    rc = new TaxRC();
                }

                error.setErrorCode(EX02);
                rc.addError(error);
                rc.setTaxValid(false);
                res.setTaxRC(rc);
            };

            completableFuture = executeAsync(() -> taxValidationService.validate(finalResult, uavPostalCachedObject),
                    validationContext, completableFuture, taxErrorSetter);
        }

        if (BooleanUtils.isTrue(checkE911)) {
            BiConsumer<UavPostalValidationResult, ValidationError> e911ErrorSetter = (res, error) -> {
                E911RC rc = res.getE911RC();
                if (rc == null) {
                    rc = new E911RC();
                }

                error.setErrorCode(EX03);
                rc.addError(error);
                rc.setE911Valid(false);
                res.setE911RC(rc);
            };

            completableFuture = executeAsync(() -> e911ValidationService.validate(finalResult, uavPostalCachedObject),
                    validationContext, completableFuture, e911ErrorSetter);
        }

        try {

            UavPostalValidationResult uavPostalValidationResult = (completableFuture == null) ? finalResult : completableFuture.get();

            if (!isAlreadyExistInCache || updateInCacheAfterRevalidate) {
                if (!CollectionUtils.isEmpty(uavPostalValidationResult.getResults())) {
                    uavPostalCachedObject.setPostalValidationResult(uavPostalValidationResult);
                    uavPostalCachedObject.setIsCustomAddress(uavPostalValidationResult.getIsCustom());
                    uavPostalCachedObject.setPostalValidationResultUpdated(true);

                    SecurityContext context = SecurityContextHolder.getContext();
                    Authentication authentication = context.getAuthentication();
                    if (authentication != null && !authenticationTrustResolver.isAnonymous(authentication)) {
                        uavPostalCachedObject.setSource(authentication.getName());
                    } else {
                        uavPostalCachedObject.setSource(validationContext.getClientName());
                    }
                    UavPostalCachedObject updatedUavPostalCachedObject = uavCacheService.updateUavPostalCachedObject(uavPostalCachedObject);
                    //if after update we found that there's custom address, we map all keys to that custom and
                    //we need to return validation result from that custom.
                    if (updatedUavPostalCachedObject != null && BooleanUtils.isTrue(updatedUavPostalCachedObject.getIsCustomAddress())) {
                        uavPostalValidationResult = updatedUavPostalCachedObject.getPostalValidationResult();
                    }
                } else {
                    log.warn("The address " + postalRequest + " was not added to the cache because the mappings are empty");
                }
            }

            return uavPostalValidationResult;
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage());
            if (e.getCause() instanceof ProviderException) {
                throw (ProviderException) e.getCause();
            }
            throw new PostalValidationException(e);
        }
    }

    @Override
    public UavPostalCachedObject lookup(ValidationContext validationContext, PostalValidationRequest postalValidationRequest) {
        validate(validationContext, postalValidationRequest);
        UavPostalCachedObject uavPostalCachedObject = uavCacheService.getUavPostalCachedObject(postalValidationRequest);
        if (BooleanUtils.isTrue(uavPostalCachedObject.getIsCustomAddress()))
            addResultCodeToCustomAddress(uavPostalCachedObject);
        return uavPostalCachedObject;
    }

    @Override
    public UavPostalCachedObject lookupByUid(String uid) {
        UavPostalCachedObject uavPostalCachedObjectByUid = uavCacheService.getUavPostalCachedObjectByUid(uid);
        if (BooleanUtils.isTrue(uavPostalCachedObjectByUid.getIsCustomAddress()))
            addResultCodeToCustomAddress(uavPostalCachedObjectByUid);
        return uavPostalCachedObjectByUid;
    }

    @Override
    public SearchCustomAddressesResponse searchCustomByParams(Integer pageSize, Integer pageNumber, SearchAddressType searchAddressType, String country, String state, String city, String zip, String fullStreetAddress) {
        SearchCustomAddressesResponse response = uavCacheService.findCustomByParams(pageSize, pageNumber, searchAddressType, country, state, city, zip, fullStreetAddress);
        response.getResult().stream().filter(cachedObject -> BooleanUtils.isTrue(cachedObject.getIsCustomAddress()))
                .forEach(this::addResultCodeToCustomAddress);
        return response;
    }

    @Override
    public UavPostalCachedObject deleteAddress(PostalRequest postalRequest) {
        UavPostalCachedObject uavPostalCachedObject = uavCacheService.getUavPostalCachedObject(postalRequest);
        uavCacheService.deleteUavPostalCachedObject(uavPostalCachedObject);
        return uavPostalCachedObject;
    }

    @Override
    public UavPostalCachedObject deleteAddressByUID(String uid) {
        return uavCacheService.deleteAddressByUID(uid);
    }


    private CompletableFuture<UavPostalValidationResult> executeAsync(Supplier<UavPostalValidationResult> supplier,
                                                                      ValidationContext context,
                                                                      CompletableFuture<UavPostalValidationResult> result,
                                                                      BiConsumer<UavPostalValidationResult, ValidationError> errorSetter) {
        CompletableFuture<UavPostalValidationResult> task = uavAsyncExecutor.supplyAsync(
                new ExecutionWrapper(supplier, context, errorSetter, alarmService));
        return (result == null) ?
                task :
                result.thenCombine(
                        task,
                        (r1, r2) -> {
                            ValidationUtils.merge(r1, r2);
                            return r1;
                        });
    }

    private boolean anyCorrectionsMade(PostalRC postalRC) {
        return postalRC.getErrors().stream()
                .map(ValidationError::getErrorCode)
                .anyMatch(e -> e == VE05);
    }

    private boolean containsFatalError(PostalRC postalRC) {
        return postalRC.getErrors().stream()
                .map(ValidationError::getErrorCode)
                .anyMatch(e -> e == VE06 || e == VE12 || e == VE14 || e == EX06);
    }

    private void addResultCodeToCustomAddress(UavPostalCachedObject cachedObject) {
        Set<PostalValidationResult.Result> resultCodes = cachedObject.getPostalValidationResult().getResults();
        if (CollectionUtils.isEmpty(resultCodes)) {
            resultCodes = new TreeSet<>();
        }
        resultCodes.add(PostalValidationResult.Result.AV24);
        cachedObject.getPostalValidationResult().setResults(resultCodes);
    }

    /**
     * This wrapper wraps Supplier that produces UavPostalValidationResult with setting/clearing MDC for logging.
     */
    private static class ExecutionWrapper implements Supplier<UavPostalValidationResult> {

        private final Supplier<UavPostalValidationResult> underlying;
        private final UUID requestId;
        private final String clientRequestID;
        private final String clientName;
        private final String clientInstance;
        private final BiConsumer<UavPostalValidationResult, ValidationError> errorSetter;
        private final AlarmService alarmService;

        ExecutionWrapper(Supplier<UavPostalValidationResult> underlying, ValidationContext context, BiConsumer<UavPostalValidationResult, ValidationError> errorSetter, AlarmService alarmService) {
            this.underlying = underlying;
            this.requestId = context.getRequestId();
            this.clientRequestID = context.getClientRequestID();
            this.clientName = context.getClientName();
            this.clientInstance = context.getClientInstance();
            this.errorSetter = errorSetter;
            this.alarmService = alarmService;
        }

        @Override
        public UavPostalValidationResult get() {
            try {
                LoggingUtils.putToMdc(LoggingUtils.REQUEST_ID_KEY, requestId.toString());
                LoggingUtils.putToMdc(LoggingUtils.CLIENT_REQUEST_ID, clientRequestID);
                LoggingUtils.putToMdc(LoggingUtils.CLIENT_NAME, clientName);
                LoggingUtils.putToMdc(LoggingUtils.CLIENT_INSTANCE, clientInstance);
                return underlying.get();
            } catch (Exception e) {
                if (e instanceof ProviderException) {
                    alarmService.sendAlarm((ProviderException) e);
                }
                ValidationError validationError = new ValidationError();
                validationError.setErrorDescription(e.getMessage());
                UavPostalValidationResult result = new UavPostalValidationResult();
                errorSetter.accept(result, validationError);
                return result;
            } finally {
                // Tear down MDC, because the thread could be reused.
                LoggingUtils.clearMdc();
            }
        }
    }
}
