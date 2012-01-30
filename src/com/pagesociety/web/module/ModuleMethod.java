package com.pagesociety.web.module;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import com.pagesociety.persistence.Entity;
import com.pagesociety.util.OBJECT;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.gateway.Form;
import com.pagesociety.web.gateway.RawCommunique;
import com.pagesociety.web.upload.MultipartForm;

public class ModuleMethod
{
	private Method method;
	private Class<?>[] ptypes;
	private String[] pnames;
	private Class<?>[] exceptionTypes;
	private Class<?> returnType;
	private boolean transaction_protected;
	

	public void init(Method method, Export export) throws InitializationException
	{
		this.method = method;
		this.ptypes = method.getParameterTypes();
		this.pnames = export.ParameterNames();
		this.exceptionTypes = method.getExceptionTypes();
		this.returnType = method.getReturnType();

		validate_parameter_types();
	
		TransactionProtect transaction_protect = method.getAnnotation(TransactionProtect.class);
		if(transaction_protect != null)
			setTransactionProtected(true);
		else
			setTransactionProtected(false);
	}
	

	private void validate_parameter_types() throws InitializationException
	{
		if (this.ptypes.length == 0 || this.ptypes[0] != UserApplicationContext.class)
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
			throws Throwable
	{
		try
		{
			return method.invoke(module, args);
		}
		
		catch (InvocationTargetException e)
		{
			e.printStackTrace();
			throw (Throwable) e.getCause();
		}
		catch (IllegalArgumentException e)
		{
			StringBuffer b = new StringBuffer();
			b.append("Error invoking " + module.getClass().getSimpleName() + "/" + getName() + " with arguments [");
			for (int i = 0; i < args.length; i++)
			{
				b.append(args[i]);
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
		
		if(!(arguments[0] instanceof UserApplicationContext))
		{
			System.err.println("BAD FIRST ARGUMENT FOR METHOD. NOT A USERAPPLICATIONCONTEXT COMPATIBLE CLASS");
			return false;
		}
		
		for(int i = 1;i < arguments.length ;i++)
		{
			Object arg = arguments[i];
			if(arg == null)
				continue;
			Class<?> arg_class = arg.getClass();
			if (!compatible_classes(arg_class,ptypes[i]) && !compatible_classes(ptypes[i], arg_class))
			{
				if(coerce_arg(arguments,i,arg_class,ptypes[i]))
					continue;
				System.out.println(method.getName()+" NON COMPATIBLE CLASSES FOR METHOD SIG. "+arg.getClass().getSimpleName()+" "+ptypes[i].getSimpleName());
				return false;
			}
		}
		return true;
	}
	
	private boolean coerce_arg(Object[] args,int idx,Class<?> arg_class, Class<?> ptype)
	{
		if((arg_class == Double.class || arg_class == double.class) && (ptype == Float.class || ptype == float.class))
		{
			args[idx] = new Float((Double)args[idx]);
			return true;
		}
		else if((arg_class == long.class || arg_class == Long.class) && (ptype == Date.class))
		{
			args[idx] = new Date((Long)args[idx]);
			return true;
		}
		else if((arg_class == LinkedHashMap.class || arg_class == HashMap.class) && (ptype == Entity.class))
		{
			Map arg = (Map)args[idx];
			String ps_clazz = (String)arg.get("_ps_clazz");
			if(ps_clazz != null)
			{
				if(ps_clazz.equals("Entity"))
					args[idx] = coerce_json_to_entity(args[idx]);//thos could be coerce to bean if we do it on the client (ps_webapp.js) as well
				return true;
			}
			return false;
		}

		return false;
	}
	
	//this should be moved to JSONEncoder or something//
	private Object coerce_json_to_entity(Object o)
	{
		if(o instanceof Map)
		{
			Map<String,Object> map = (Map<String,Object>)o;
			String ps_clazz = (String)map.get("_ps_clazz");
			if(ps_clazz != null)
			{
				if(ps_clazz.equals("Entity"))
				{
					Map<String,Object> atts = (Map<String,Object>)map.get("attributes");
					atts.remove("_object_id");
					if(atts == null)
						atts = new HashMap<String, Object>();
					for(String k:atts.keySet())
					{
						Object val = atts.get(k);
						if(val instanceof List)
						{
							List<Object> lv = (List<Object>)val;
							for(int i = 0; i < lv.size();i++)
								lv.set(i, coerce_json_to_entity(lv.get(i)));
						}
						else
						{
							atts.put(k,coerce_json_to_entity(val));
						}
					}
					
					Entity e = new Entity();
					e.setType((String)map.get("type"));
					e.setId(new Long((Integer)map.get("id")));
					e.setAttributes((Map<String,Object>)atts);
					List<String> dirty_attributes = (List<String>)map.get("dirtyAttributes");
					//if(dirty_attributes != null)
					//	e.setDirtyAttributes(dirty_attributes);
					return e;
				}
			}
			else
			{
				System.out.println("RETURNING MAP "+map);
				return map;
			}
		}
		return o;
	}

	
	private static boolean compatible_classes(Class<?> c1,Class<?> c2)
	{
		if((c1 == c2) ||//TODO: i dont think we need the rest of these checks anymore
				(c1 == Long.class 	 && c2 	== long.class) 	   ||
				(c1 == Integer.class && c2 	== int.class) 	   ||
				(c1 == ArrayList.class 	 && c2 	== List.class) ||
				(c1 == Boolean.class && c2 	== boolean.class)  ||
				(c1 == Float.class   && c2 	== float.class)    ||
				(c1 == Integer.class && c2 	== long.class)     ||
				(c1 == Float.class && c2 	== double.class)   ||
				((c1 == HashMap.class || c1 == LinkedHashMap.class) && c2  == Map.class))
			return true;
		
		return false;		
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

	

	
	public static boolean isValidParamType(Class<?> c) 
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
			(c == OBJECT.class)		||
			(c == HashMap.class)	||
			(c == LinkedHashMap.class)	||
			(c == Date.class)		||
			(c == Boolean.class)	||
			(c == boolean.class)	||
			(c == MultipartForm.class)  ||
			(c == Form.class) 			||
			(c == RawCommunique.class)  ||
			(c == byte.class && c.isArray()))
				return true;
		
		return false;
	}


	public void setTransactionProtected(boolean b) 
	{
		transaction_protected = b;	
	}
	
	public boolean isTransactionProtected() 
	{
		return transaction_protected;	
	}
}
