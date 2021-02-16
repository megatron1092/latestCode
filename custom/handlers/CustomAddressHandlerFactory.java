package net.intermedia.uav.postal.custom.handlers;

public interface CustomAddressHandlerFactory {
    CustomAddressHandler getHandler(String countryFromRequest);
}
