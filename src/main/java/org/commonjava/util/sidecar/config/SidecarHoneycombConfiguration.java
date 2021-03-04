package org.commonjava.util.sidecar.config;

import io.quarkus.runtime.Startup;
import org.commonjava.o11yphant.honeycomb.config.HoneycombConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Collections.EMPTY_MAP;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.commonjava.util.sidecar.metrics.MetricFieldsConstants.HEADERS;

@ApplicationScoped
@Startup
public class SidecarHoneycombConfiguration
                implements HoneycombConfiguration
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private Set<String> fieldSet;

    @Override
    public Set<String> getFieldSet()
    {
        return fieldSet;
    }

    @Inject
    SidecarHoneycombConfObj confObj;

    private Map<String, SLIFunction> functionMap = EMPTY_MAP;

    private Map<String, Integer> sampleRates = EMPTY_MAP; // made from functionMap for convenience

    @PostConstruct
    void init()
    {
        String functions = confObj.functions.orElse( "" );
        if ( isNotBlank( functions ) )
        {
            logger.info( "Set honeycomb configuration, functions: {}", functions );
            Map<String, SLIFunction> m = new HashMap<>();
            String[] toks = functions.split( "," );
            for ( String s : toks )
            {
                SLIFunction f = SLIFunction.parse( s );
                m.put( f.name, f );
            }
            functionMap = Collections.unmodifiableMap( m );

            sampleRates = new HashMap<>();
            functionMap.entrySet().forEach( et -> sampleRates.put( et.getKey(), et.getValue().sampleRate ) );
        }

        fieldSet = Collections.unmodifiableSet( new HashSet<>( Arrays.asList( HEADERS ) ) );
    }

    @Override
    public String getServiceName()
    {
        return "sidecar";
    }

    @Override
    public boolean isEnabled()
    {
        return confObj.enabled.orElse( false );
    }

    @Override
    public String getWriteKey()
    {
        return confObj.writeKey.orElse( null );
    }

    @Override
    public String getDataset()
    {
        return confObj.dataset.orElse( null );
    }

    @Override
    public Integer getBaseSampleRate()
    {
        return confObj.baseSampleRate.orElse( 0 );
    }

    @Override
    public String getNodeId()
    {
        return null;
    }

    @Override
    public boolean isConsoleTransport()
    {
        return confObj.consoleTransport.orElse( false );
    }

    public static class SLIFunction
    {
        Pattern pattern;

        String name;

        Integer sampleRate;

        public static SLIFunction parse( String s )
        {
            String[] toks = s.split( "\\|" );

            SLIFunction ret = new SLIFunction();
            ret.pattern = Pattern.compile( toks[0].trim() );
            ret.name = toks[1].trim();
            if ( toks.length > 2 )
            {
                ret.sampleRate = Integer.parseInt( toks[2].trim() );
            }
            return ret;
        }

        @Override
        public String toString()
        {
            return "SLIFunction{" + "pattern=" + pattern + ", name='" + name + '\'' + ", sampleRate=" + sampleRate + '}';
        }
    }

    public String getFunctionName( String path )
    {
        for ( SLIFunction f : functionMap.values() )
        {
            if ( f.pattern.matcher( path ).matches() )
            {
                return f.name;
            }
        }
        return null;
    }

    public Map<String, SLIFunction> getFunctionMap()
    {
        return functionMap;
    }

    @Override
    public Map<String, Integer> getSpanRates()
    {
        return sampleRates;
    }

}
