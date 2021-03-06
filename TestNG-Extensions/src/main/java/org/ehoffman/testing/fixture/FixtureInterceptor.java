package org.ehoffman.testing.fixture;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ehoffman.module.Module;
import org.ehoffman.module.ModuleProvider;
import org.ehoffman.testing.fixture.services.FactoryUtil;
import org.ehoffman.testing.testng.Interceptor;
import org.ehoffman.testng.extensions.Fixture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IMethodInstance;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.internal.MethodInstance;

public class FixtureInterceptor implements Interceptor {
  
  private static Logger logger = LoggerFactory.getLogger(FixtureInterceptor.class);
  
  private static Map<ITestNGMethod, Set<Class<? extends Module<?>>>> testNGMethodToSetOfModuleClassesForSingleInvocation = Collections.synchronizedMap(new IdentityHashMap<ITestNGMethod, Set<Class<? extends Module<?>>>>());
  private static final Set<Class<? extends Module<?>>> emptySet = new HashSet<Class<? extends Module<?>>>(); 

  
  private Iterator<Set<Class<? extends Module<?>>>> fixtureIterator(Fixture fixture) {
    Class<? extends ModuleProvider<?>>[] moduleArray = fixture.factory();
    return FixtureContainer.getDotProductModuleCombinations(Arrays.asList(moduleArray), fixture.destructive());
  }
  
  /**
   * 
   * @param instance the inputed IMethodInstance that has not yet been evaluated
   * @param context
   * @return a set containing either the IMethodInstance that was passed in, or set or ITestMethods with a corresponding key in the {@link FixtureInterceptor#testNGMethodToSetOfModuleClassesForSingleInvocation}
   * map that has a value of the Set or modules that particular instance of IMethodInstance should be run with.  The services the {@link Module}s provide with be available via the {@link FixtureContainer} to the test.
   */
  private List<IMethodInstance> calculateRuns(IMethodInstance instance){
    List<IMethodInstance> output = new ArrayList<IMethodInstance>();
    Method method = instance.getMethod().getConstructorOrMethod().getMethod();
    if (method != null && method.getAnnotation(Fixture.class) != null) {
      Fixture fixture = method.getAnnotation(Fixture.class);
      Iterator<Set<Class<? extends Module<?>>>> fixtureIterator = fixtureIterator(fixture);
      while (fixtureIterator.hasNext()) {
        ITestNGMethod testNGMethod = instance.getMethod().clone();
        IMethodInstance newMultiInstance = new MethodInstance(testNGMethod);
        testNGMethodToSetOfModuleClassesForSingleInvocation.put(testNGMethod, fixtureIterator.next());
        logger.info("MethodInstance "+newMultiInstance+" with method "+testNGMethod.toString()+" with modules of "+testNGMethodToSetOfModuleClassesForSingleInvocation.get(testNGMethod));
        output.add(newMultiInstance);
      }
    } else {
      output.add(instance);
    }
    //logger.info("added all "+testNGMethodToSetOfModuleClassesForSingleInvocation);
    return output;
  }



  @Override
  public List<IMethodInstance> intercept(List<IMethodInstance> methods) {
    List<IMethodInstance> output = new ArrayList<IMethodInstance>();
    for (IMethodInstance instance : methods){
      output.addAll(calculateRuns(instance));
    }
    return output;
  }

  @Override
  public List<String> getConfigErrorMessages() {
    return new ArrayList<String>();
  }

  @Override
  public void beforeInvocation(ITestResult testResult) {
    logger.info(FixtureInterceptor.class.getSimpleName()+" in beforeInvocation");
    ITestNGMethod testNGmethod = testResult.getMethod();
    Set<Class<? extends Module<?>>> moduleClasses = testNGMethodToSetOfModuleClassesForSingleInvocation.get(testNGmethod);
    System.out.println("Module classes should be: "+moduleClasses);
    if (moduleClasses != null){
      FixtureContainer.setModuleClasses(moduleClasses);
    } else {
      FixtureContainer.setModuleClasses(emptySet);
      FixtureContainer.wipeFixture();
    }
  }

  @Override
  public void afterInvocation(ITestResult testResult) {
    if (testNGMethodToSetOfModuleClassesForSingleInvocation.get(testResult.getMethod()) != null) {
      Set<Class<? extends Module<?>>> classes = testNGMethodToSetOfModuleClassesForSingleInvocation.get(testResult.getMethod());
      ArrayList<String> names = new ArrayList<String>();
      for (Class<?> klass : classes){
        names.add(klass.getSimpleName());
      }
      Collections.sort(names);
      testResult.setAttribute("module providers", names);
    }
    logger.info(FixtureInterceptor.class.getSimpleName()+" in afterInvocation");
    FixtureContainer.wipeFixture();
  }

  @Override
  public void shutdown() {
    FixtureContainer.destroyAll();
    FactoryUtil.destroy();
  }
  
}
