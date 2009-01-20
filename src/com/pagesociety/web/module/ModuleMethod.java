package com.pagesociety.web.module;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.gateway.Form;
import com.pagesociety.web.upload.MultipartForm;

public class ModuleMethod
{
	private Method method;
	private Class<?>[] ptypes;
	private String[] pnames;
	private Class<?>[] exceptionTypes;
	private Class<?> returnType;

	

	public void init(Method method, Export export) throws InitializationException
	{
		this.method = method;
		this.ptypes = method.getParameterTypes();
		this.pnames = export.ParameterNames();
		this.exceptionTypes = method.getExceptionTypes();
		this.returnType = method.getReturnType();
		
		validate_parameter_types();
	}
	

	private void validate_parameter_types() throws InitializationException
	{
		if (this.ptypes[0] != UserApplicationContext.class)
			throw new InitializationException("The first argument of every module method must be UserApplicationContext");
		for(int i = 1;i < ptypes.length;i++)
		{
			Class p_type = ptypes[i];
			if(isValidParamType(p_type))
				continue;
			throw new InitializationException("UNSUPPORTED PARAMETER TYPE: "+p_type.getName()+" FOR METHOD "+getName());
		}
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

	public String[] getParameterNames()
	{
		return pnames;
	}
	
	public Class<?>[] getExceptionTypes()
	{
		return exceptionTypes;
	}

	public Class<?> getReturnType()
	{
		return returnType;
	}

	public Object invoke(Module module, Object[] args)
			throws Exception
	{
		try
		{
			return method.invoke(module, args);
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

	public boolean isValidForArgs(Object[] arguments)
	{
		if (ptypes.length != arguments.length)
			return false;
		
		for(int i = 0;i < arguments.length ;i++)
		{
			Object arg = arguments[i];
			if(arg == null)
				continue;
			if (!compatible_classes(arg.getClass(),ptypes[i]) && !compatible_classes(ptypes[i], arg.getClass()))
				return false;
		}
		return true;
	}
	
	private static boolean compatible_classes(Class c1,Class c2)
	{
		if((c1 == c2) ||
				(c1 == Long.class 	 && c2 	== long.class) 	   ||
				(c1 == Integer.class && c2 	== int.class) 	   ||
				(c1 == ArrayList.class 	 && c2 	== List.class) ||
				(c1 == Boolean.class && c2 	== boolean.class)  ||
				(c1 == Float.class   && c2 	== float.class)    ||
				(c1 == Integer.class && c2 	== long.class)     ||
				(c1 == Float.class && c2 	== double.class))
			return true;
		
		return false;		
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

	

	
	public static boolean isValidParamType(Class c) 
	{

		if((c == UserApplicationContext.class)||
			(c == String.class)		||
			(c == Entity.class) 	||
			(c == Long.class)   	|| 
			(c == long.class)   	|| 
			(c == Integer.class) 	||
			(c == int.class)	 	||
			(c == Float.class) 		||
			(c == float.class) 		||
			(c == Double.class)		||
			(c == double.class)		||
			(c == List.class)  		||
			(c == ArrayList.class)	||
			(c == Map.class)		||
			(c == HashMap.class)	||
			(c == Date.class)		||
			(c == Boolean.class)	||
			(c == boolean.class)	||
			(c == MultipartForm.class)  ||
			(c == Form.class) 			||
			(c == byte.class && c.isArray()))
				return true;
		
		return false;
	}
}
