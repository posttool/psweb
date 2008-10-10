package com.pagesociety.web.module;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;

public class ModuleMethod
{
	private Method method;
	private Class<?>[] ptypes;
	private Class<?>[] exceptionTypes;
	private Class<?> returnType;

	public void reflect(Method method) throws InitializationException
	{
		this.method = method;
		this.ptypes = method.getParameterTypes();
		this.exceptionTypes = method.getExceptionTypes();
		this.returnType = method.getReturnType();
		if (this.ptypes[0] != UserApplicationContext.class)
			throw new InitializationException("The first argument of every module method must be UserApplicationContext");
	}

	public String getName()
	{
		return method.getName();
	}

	public Method getMethod()
	{
		return method;
	}

	public Class<?>[] getParameterTypes()
	{
		return ptypes;
	}

	public Class<?>[] getExceptionTypes()
	{
		return exceptionTypes;
	}

	public Class<?> getReturnType()
	{
		return returnType;
	}

	public Object invoke(Module module, UserApplicationContext user_context, Object[] args)
			throws Exception
	{
		Object[] args_with_user = new Object[args.length + 1];
		System.arraycopy(args, 0, args_with_user, 1, args.length);
		args_with_user[0] = user_context;
		try
		{
			return method.invoke(module, args_with_user);
		}
		catch (InvocationTargetException e)
		{
			throw (Exception) e.getCause();
		}
		catch (IllegalArgumentException e)
		{
			StringBuffer b = new StringBuffer();
			b.append("Error invoking " + module.getClass().getSimpleName() + "/" + getName() + " with arguments [");
			for (int i = 0; i < args.length; i++)
			{
				b.append(args[i].toString());
				if (i != args.length - 1)
					b.append(", ");
			}
			b.append("]. ");
			b.append("Arguments of type (");
			for (int i = 0; i < args.length; i++)
			{
				if (args[i] == null)
					b.append("?");
				else
					b.append(args[i].getClass().getSimpleName());
				if (i != args.length - 1)
					b.append(", ");
			}
			b.append(") do not match (");
			for (int i = 0; i < ptypes.length; i++)
			{
				b.append(ptypes[i].getSimpleName());
				if (i != args.length - 1)
					b.append(", ");
			}
			b.append(")");
			throw new RuntimeException(b.toString(), e);
		}
		catch (IllegalAccessException iae)
		{
			throw new WebApplicationException("No permission to access.", iae);
		}
	}

	public boolean returnsVoid()
	{
		return (method.getReturnType() == Void.class);
	}

	public boolean isValid(Object[] arguments)
	{
		if (ptypes.length != arguments.length)
			return false;
		// TODO compare argument types w/ parameterTypes
		return true;
	}

	public Object[] coerceArgs(Object[] args) throws WebApplicationException
	{
		if (args.length != ptypes.length - 1)
			throw new WebApplicationException("ModuleMethod INCORRECT # OF ARGS");
		Object[] typed_args = new Object[args.length];
		Type[] gen_ptypes = method.getGenericParameterTypes();
		for (int i = 1; i < ptypes.length; i++)
		{
			Class<?> ptype = ptypes[i];
			Object arg = args[i-1];
			if (arg == null || arg.getClass() == ptype)
			{
				typed_args[i-1] = arg;
			}
			else if (ptype == List.class)
			{
				if (arg instanceof String)
				{
					String[] a_args = ((String) arg).split(",");
					ParameterizedType pgtype = (ParameterizedType) gen_ptypes[i];
					Class<?> pgclass = (Class<?>) pgtype.getActualTypeArguments()[0];
					List<Object> typed_array_values = new ArrayList<Object>(a_args.length);
					for (int j = 0; j < a_args.length; j++)
					{
						typed_array_values.add(coerseStringToObject(pgclass, a_args[j]));
					}
					typed_args[i-1] = typed_array_values;
				}
				else if (arg instanceof JSONArray)
				{
					JSONArray a_args = (JSONArray) arg;
					ParameterizedType pgtype = (ParameterizedType) gen_ptypes[i];
					Class<?> pgclass = (Class<?>) pgtype.getActualTypeArguments()[0];
					List<Object> typed_array_values = new ArrayList<Object>(a_args.length());
					for (int j = 0; j < a_args.length(); j++)
					{
						try{
							typed_array_values.add(coerseStringToObject(pgclass, String.valueOf(a_args.get(j))));
						}catch(JSONException jse)
						{
							jse.printStackTrace();
							throw new WebApplicationException("GOT A JSON EXCEPTION WHEN COERCING ARGS",jse);
						}
					}
					typed_args[i-1] = typed_array_values;
				}
			}
			else if (ptype.isArray())
			{
				throw new WebApplicationException("COERCABLE MODULE METHODS MUST SPECIFY LIST PARAMETERS (NOT ARRAYS). ");
			}
			else if (arg.getClass() == String.class)
			{
				typed_args[i-1] = coerseStringToObject(ptype, (String) arg);
			}
			else if (ptype == Date.class && arg.getClass() == Long.class)
			{
				typed_args[i-1] = new Date((Long) arg);
			}
			else
			{
				typed_args[i-1] = arg;
			}
		}
		return typed_args;
	}

	private static Object coerseStringToObject(Class<?> ptype, String arg) throws WebApplicationException
	{
		try
		{
			if (ptype == boolean.class || ptype == Boolean.class)
			{
				return Boolean.parseBoolean(arg);
			}
			else if (ptype == int.class || ptype == Integer.class)
			{
				return Integer.parseInt(arg);
			}
			else if (ptype == long.class || ptype == Long.class)
			{
				return Long.parseLong(arg);
			}
			else if (ptype == float.class || ptype == Float.class)
			{
				return Float.parseFloat(arg);
			}
			else if (ptype == String.class)
			{
				return arg;
			}
			else if (ptype == Date.class)
			{
				return new Date(Long.parseLong(arg));
			}
			else if (ptype == Entity.class)
			{
				// String[] eid = arg.split("_");
				// EntityDefinition def = _store.getEntityDefinition(eid[0]);
				// long id = Long.parseLong(eid[1]);
				// return new Entity(def, id);
				throw new WebApplicationException("COERCABLE MODULE METHODS CANT SPECIFY ENTITIES YET");
			}
			return null;
		}
		catch (Exception e)
		{
			throw new RuntimeException("UNABLE TO COERSE '" + arg + "' TO '" + ptype.getName() + "'", e);
		}
	}

	public String toString()
	{
		StringBuffer b = new StringBuffer();
		b.append(returnType.getSimpleName());
		b.append(" ");
		b.append(getName());
		b.append("(");
		for (int j = 0; j < ptypes.length; j++)
		{
			Class<?> ptype = ptypes[j];
			b.append(ptype.getSimpleName());
			if (j != ptypes.length - 1)
				b.append(",");
		}
		b.append(")");
		// if (exceptionTypes.length != 0)
		// {
		// b.append(" throws ");
		// for (int i = 0; i < exceptionTypes.length; i++)
		// {
		// b.append(exceptionTypes[i].getSimpleName());
		// if (i != exceptionTypes.length - 1)
		// b.append(", ");
		// }
		// }
		return b.toString();
	}
}
