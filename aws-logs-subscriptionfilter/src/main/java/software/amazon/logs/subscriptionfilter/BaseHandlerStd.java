package software.amazon.logs.subscriptionfilter;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.*;

import static java.util.Objects.requireNonNull;


public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    private final CloudWatchLogsClient cloudWatchLogsClient;

    protected BaseHandlerStd() {
        this(ClientBuilder.getClient());
    }

    protected BaseHandlerStd(CloudWatchLogsClient cloudWatchLogsClient) {
        this.cloudWatchLogsClient = requireNonNull(cloudWatchLogsClient);
    }

    private CloudWatchLogsClient getCloudWatchLogsClient() {
        return cloudWatchLogsClient;
    }

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        return handleRequest(
                proxy,
                request,
                callbackContext != null ? callbackContext : new CallbackContext(),
                proxy.newProxy(ClientBuilder::getClient),
                logger
        );
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudWatchLogsClient> proxyClient,
            final Logger logger);

    protected boolean doesResourceExist(final ProxyClient<CloudWatchLogsClient> proxyClient,
                                        final ResourceModel model) throws AwsServiceException {
        final DescribeSubscriptionFiltersRequest translateToReadRequest = Translator.translateToReadRequest(model);
        final DescribeSubscriptionFiltersResponse response;
        try {
            response = proxyClient.injectCredentialsAndInvokeV2(translateToReadRequest, proxyClient.client()::describeSubscriptionFilters);
            return !response.subscriptionFilters().isEmpty();
        } catch (final AwsServiceException e) {
            BaseHandlerException newException = Translator.translateException(e);
            if (newException instanceof CfnNotFoundException) {
                return false;
            }
            throw e;
        }
    }

    protected void logExceptionDetails(Exception e, Logger logger, final String stackId) {
        logger.log(String.format("Stack with ID: %s got exception: %s Message: %s Cause: %s", stackId,
                e.toString(), e.getMessage(), e.getCause()));
    }

    protected void handleException(Exception e, Logger logger, final String stackId) {
        if (e instanceof InvalidParameterException) {
            logExceptionDetails(e, logger, stackId);
            throw new CfnInvalidRequestException(e);
        } else if (e instanceof ResourceNotFoundException) {
            logExceptionDetails(e, logger, stackId);
            throw new CfnNotFoundException(e);
        } else if (e instanceof ServiceUnavailableException) {
            logExceptionDetails(e, logger, stackId);
            throw new CfnServiceInternalErrorException(e);
        }
    }

}
