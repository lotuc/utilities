package lotuc.quartz.util.jndi;

import java.util.Hashtable;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;

public class SimpleNamingContextBuilder implements InitialContextFactoryBuilder {

  private final Hashtable<String, Object> boundObjects;

  public SimpleNamingContextBuilder(Hashtable<String, Object> boundObjects) {
    this.boundObjects = boundObjects;
  }

  @Override
  @SuppressWarnings("unchecked")
  public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment) {
    return env -> new SimpleNamingContext("", this.boundObjects, (Hashtable<String, Object>) env);
  }
}
