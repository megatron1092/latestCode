package net.intermedia.uav.postal.custom.handlers;

import net.intermedia.uav.country.Country;
import net.intermedia.uav.postal.custom.handlers.international.AUCustomAddressHandler;
import net.intermedia.uav.postal.custom.handlers.international.DECustomAddressHandler;
import net.intermedia.uav.postal.custom.handlers.international.ITCustomAddressHandler;
import net.intermedia.uav.postal.custom.handlers.international.JPCustomAddressHandler;
import net.intermedia.uav.postal.custom.handlers.international.NLCustomAddressHandler;
import net.intermedia.uav.postal.custom.handlers.international.UKCustomAddressHandler;
import net.intermedia.uav.postal.custom.handlers.us.CaCustomAddressHandler;
import net.intermedia.uav.postal.custom.handlers.us.UsCustomAddressHandler;
import org.springframework.stereotype.Service;

@Service
public class CustomAddressHandlerFactoryImpl implements CustomAddressHandlerFactory {

    @Override
    public CustomAddressHandler getHandler(String countryFromRequest){
        Country country = Country.find(countryFromRequest);
        CustomAddressHandler handler;
        switch (country) {
            case US:
            case PR:
                handler = new UsCustomAddressHandler(){};
                break;
            case CA:
                handler = new CaCustomAddressHandler();
                break;
            case UK:
                handler = new UKCustomAddressHandler();
                break;
            case NL:
                handler = new NLCustomAddressHandler();
                break;
            case DE:
                handler = new DECustomAddressHandler();
                break;
            case IT:
                handler = new ITCustomAddressHandler();
                break;
            case AU:
                handler = new AUCustomAddressHandler();
                break;
            case JP:
                handler = new JPCustomAddressHandler();
                break;
            default:
                throw new IllegalStateException("Unexpected country for CustomAddressHandler: " + countryFromRequest);
        }
        return handler;
    }
}
