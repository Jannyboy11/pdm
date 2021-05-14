package me.bristermitten.pdm.util;

import org.jetbrains.annotations.NotNull;
import xyz.janboerman.scalaloader.plugin.ScalaPluginClassLoader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class ClassLoaderReflection
{

    private static Method addUrl;
    private static final boolean scalaLoaderPresent;
    static {
        boolean present;
        try {
            Class.forName("xyz.janboerman.scalaloader.plugin.ScalaPluginClassLoader");
            present = true;
        } catch (ClassNotFoundException e) {
            present = false;
        }
        scalaLoaderPresent = present;
    }

    private ClassLoaderReflection()
    {
        throw new AssertionError("This class cannot be instantiated.");
    }

    private static Method addUrlMethod()
    {
        if (ClassLoaderReflection.addUrl == null)
        {
            final Method addURL;

            // open the classloader module for java9+ so it wont have a warning
            try
            {
                openUrlClassLoaderModule();
            }
            catch (Throwable ignored)
            {
                // ignore exception. Java 8 wont have the module, so it wont matter if we ignore it
                // cause there will be no warning
            }

            try
            {
                addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                addURL.setAccessible(true);
            }
            catch (NoSuchMethodException exception)
            {
                throw new AssertionError(exception);
            }

            ClassLoaderReflection.addUrl = addURL;
        }

        return ClassLoaderReflection.addUrl;
    }

    public static void addURL(@NotNull final URLClassLoader classLoader, @NotNull final URL url)
    {

        if (scalaLoaderPresent && classLoader instanceof ScalaPluginClassLoader)
        {
            ScalaPluginClassLoader scalaPluginClassLoader = (ScalaPluginClassLoader) classLoader;
            scalaPluginClassLoader.addUrl(url);
        }
        else
        {
            try
            {
                addUrlMethod().invoke(classLoader, url);
            }
            catch (IllegalAccessException | InvocationTargetException exception)
            {
                throw new IllegalArgumentException(exception);
            }
        }
    }

    @SuppressWarnings("JavaReflectionInvocation")
    private static void openUrlClassLoaderModule() throws Exception
    {
        // Taken from LuckPerms

        Class<?> moduleClass = Class.forName("java.lang.Module");
        Method getModuleMethod = Class.class.getMethod("getModule");
        Method addOpensMethod = moduleClass.getMethod("addOpens", String.class, moduleClass);

        Object urlClassLoaderModule = getModuleMethod.invoke(URLClassLoader.class);
        Object thisModule = getModuleMethod.invoke(ClassLoaderReflection.class);

        addOpensMethod.invoke(urlClassLoaderModule, URLClassLoader.class.getPackage().getName(), thisModule);
    }
}
