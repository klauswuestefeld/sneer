package sneer.bricks.hardware.io.prevalence.nature.impl;

import static basis.environments.Environments.my;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;

import basis.lang.CacheMap;
import basis.lang.Immutable;
import basis.lang.Producer;
import basis.lang.ProducerX;
import basis.lang.ReadOnly;
import basis.lang.types.Classes;

import sneer.bricks.hardware.io.prevalence.flag.PrevalenceFlag;
import sneer.bricks.hardware.io.prevalence.map.PrevalenceMap;
import sneer.bricks.hardware.io.prevalence.nature.Transaction;

class Bubble implements InvocationHandler {
	
	private static final PrevalenceMap PrevalenceMap = my(PrevalenceMap.class);
	
	private static final CacheMap<Object, Object> _proxiesByObject = CacheMap.newInstance();
	

	static <T> T wrapped(final Object object, final ProducerX<Object, ? extends Exception> path) {
		return (T)_proxiesByObject.get(object, new Producer<Object>() { @Override public Object produce() {
			return newProxyFor(object, path);
		}});
	}


	private static Object newProxyFor(Object object, ProducerX<Object, ? extends Exception> path) {
		if (isRegistered(object))
			path = new MapLookup(object);

		InvocationHandler handler = new Bubble(path);
		Class<?> delegateClass = object.getClass();
		return Proxy.newProxyInstance(delegateClass.getClassLoader(), Classes.allInterfacesOf(delegateClass), handler);
	}


	private Bubble(ProducerX<Object, ? extends Exception> producer) {
		_invocationPath = producer;
	}


	private final ProducerX<Object, ? extends Exception> _invocationPath;
	
	
	@Override
	public Object invoke(Object proxyImplied, Method method, Object[] args) throws Exception {
		ProducerX<Object, Exception> path = extendedPath(method, args);
		Object result = path.produce();
		return wrapIfNecessary(result, path);
	}


	private Object wrapIfNecessary(Object object, ProducerX<Object, Exception> path) {
		if (object == null) return object;
		
		Class<?> type = object.getClass();
		if (type.isArray()) return object;
		if (Collection.class.isAssignableFrom(type)) return object;
		if (Immutable.isImmutable(type)) return object;
		
		if (ReadOnly.class.isAssignableFrom(type)) return object;
		if (File.class.isAssignableFrom(type)) return object;
		
		return wrapped(object, path);
	}


	private ProducerX<Object, Exception> extendedPath(Method method, Object[] args) {
		if (!isTransaction(method))
			return new Invocation(_invocationPath, method, args);

		TransactionInvocation transaction = new TransactionInvocation(_invocationPath, method, args);
		return my(PrevalenceFlag.class).isInsidePrevalence()
			? transaction
			: withPrevayler(transaction);
	}


	private ProducerX<Object, Exception> withPrevayler(final TransactionInvocation transaction) {
		return new ProducerX<Object, Exception>() { @Override public Object produce() throws Exception {
			return PrevaylerHolder._prevayler.execute(transaction);
		}};
	}


	private boolean isTransaction(Method method) {
		if (method.getReturnType() == Void.TYPE) return true;
		if (method.getAnnotation(Transaction.class) != null) return true;
		return false;
	}
	
	
	private static boolean isRegistered(Object object) {
		return PrevalenceMap.isRegistered(object);
	}

}
