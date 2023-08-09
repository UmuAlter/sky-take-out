package com.sky.context;

public class BaseContext {
    //用于保存当前线程中的局部变量
    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }
    //获得当前登录用户id
    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
/*
该类中的 getCurrentId() 方法的作用是获取当前线程中保存的一个 Long 类型的变量。
这个方法的实现依赖于 ThreadLocal 类和其对应的 threadLocal  静态成员变量。
ThreadLocal 提供了线程局部变量的机制，每个线程都可以独立地访问自己的线程局部变量，且互不干扰。
而 threadLocal 是一个 ThreadLocal 类型的对象，用于保存当前线程中的局部变量。

在 getCurrentId() 方法中，调用 threadLocal.get() 方法即可获取当前线程中保存的 Long 类型的值。
如果当前线程还没有设置过该值，则返回 null。

需要注意的是，为了正确使用 ThreadLocal，通常在设置和获取变量之间需要进行一些操作，
例如在当前线程中设置值的方法。这里只提供了 getCurrentId() 方法，并未展示设置值的方法，因此无法完整判断该类的实现方式。
但是基于 ThreadLocal 的机制，我们可以推断，在其他地方应该有代码调用 threadLocal.set() 方法来设置当前线程中的局部变量值。
 */