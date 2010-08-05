package com.pagesociety.web.module.ecommerce.store;

import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.module.CMSParticipantModule;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.TransactionProtect;
import com.pagesociety.persistence.EntityDefinition;

import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.Types;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;



public class CatalogModule extends CMSParticipantModule
{
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);	

		
	}
	
	public void loadbang(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		bootstrap_data();
	}
	
	protected void defineSlots()
	{
		super.defineSlots();
	//	DEFINE_SLOT(SLOT_RESOURCE_MODULE,ResourceModule.class,true);
	//	DEFINE_SLOT(SLOT_USER_MODULE,UserModule.class,true);
	}

	private void bootstrap_data() throws InitializationException
	{
		/*
		 * try{
			Entity release1 = store.getEntityById(RELEASE_ENTITY,1);
			if(release1 == null)
				create_newcali();
		}catch(Exception e)
		{
			e.printStackTrace();
			throw new InitializationException("FAILED SETTING UP NEWCALI");
		}
		*/
	}
	/////////

	public static final String CAN_CREATE_WAREHOUSE 		 = "CAN_CREATE_WAREHOUSE";
	public static final String CAN_UPDATE_WAREHOUSE 		 = "CAN_UPDATE_WAREHOUSE";
	public static final String CAN_BROWSE_WAREHOUSE 		 = "CAN_BROWSE_WAREHOUSE";
	public static final String CAN_GET_WAREHOUSE 	 		 = "CAN_GET_WAREHOUSE";
	public static final String CAN_DELETE_WAREHOUSE 		 = "CAN_DELETE_WAREHOUSE";
	public static final String CAN_GET_WAREHOUSE_DEFINITION = "CAN_GET_WAREHOUSE_DEFINITION";
	public static final String CAN_CREATE_PRODUCT 		 = "CAN_CREATE_PRODUCT";
	public static final String CAN_UPDATE_PRODUCT 		 = "CAN_UPDATE_PRODUCT";
	public static final String CAN_BROWSE_PRODUCT 		 = "CAN_BROWSE_PRODUCT";
	public static final String CAN_GET_PRODUCT 	 		 = "CAN_GET_PRODUCT";
	public static final String CAN_DELETE_PRODUCT 		 = "CAN_DELETE_PRODUCT";
	public static final String CAN_GET_PRODUCT_DEFINITION = "CAN_GET_PRODUCT_DEFINITION";
	
	
	public void exportPermissions()
	{
		EXPORT_PERMISSION(CAN_CREATE_WAREHOUSE);
		EXPORT_PERMISSION(CAN_UPDATE_WAREHOUSE);
		EXPORT_PERMISSION(CAN_BROWSE_WAREHOUSE);
		EXPORT_PERMISSION(CAN_GET_WAREHOUSE);
		EXPORT_PERMISSION(CAN_DELETE_WAREHOUSE);
		EXPORT_PERMISSION(CAN_GET_WAREHOUSE_DEFINITION);	
		EXPORT_PERMISSION(CAN_CREATE_PRODUCT);
		EXPORT_PERMISSION(CAN_UPDATE_PRODUCT);
		EXPORT_PERMISSION(CAN_BROWSE_PRODUCT);
		EXPORT_PERMISSION(CAN_GET_PRODUCT);
		EXPORT_PERMISSION(CAN_DELETE_PRODUCT);
		EXPORT_PERMISSION(CAN_GET_PRODUCT_DEFINITION);	
	
	}

	/* BEGIN WAREHOUSE */
	
	
	@Export(ParameterNames 	= {"warehouse"})
	@TransactionProtect
	public Entity CreateWarehouse(UserApplicationContext uctx,Entity warehouse) throws WebApplicationException,PersistenceException
	{
		VALIDATE_TYPE(WAREHOUSE_ENTITY, warehouse);
		VALIDATE_NEW_INSTANCE(warehouse);
		
		 String name = (String)warehouse.getAttribute(WAREHOUSE_FIELD_NAME);
		 String code = (String)warehouse.getAttribute(WAREHOUSE_FIELD_CODE);

		return CreateWarehouse(uctx,name,code);
	}
	
	@Export(ParameterNames={"name","code"})
	@TransactionProtect
	public Entity CreateWarehouse(UserApplicationContext uctx,String name,String code) throws WebApplicationException,PersistenceException
	{
		Entity caller = (Entity)uctx.getUser();
		GUARD(caller, CAN_CREATE_WAREHOUSE,GUARD_TYPE,WAREHOUSE_ENTITY,
		 	WAREHOUSE_FIELD_NAME,name,
		 	WAREHOUSE_FIELD_CODE,code
					);
		
		return createWarehouse(caller,name,code);

	}

	
	public Entity createWarehouse(Entity creator,String name,String code) throws WebApplicationException,PersistenceException
	{
		
		Entity warehouse =	doCreate(WAREHOUSE_ENTITY,
	    				creator,
		 	WAREHOUSE_FIELD_NAME,name,
		 	WAREHOUSE_FIELD_CODE,code
		);
		return warehouse;		
	}
	
	@Export(ParameterNames 	= {"warehouse"})
	@TransactionProtect
	public Entity UpdateWarehouse(UserApplicationContext uctx,Entity warehouse) throws WebApplicationException,PersistenceException
	{
		VALIDATE_TYPE(WAREHOUSE_ENTITY,warehouse);
		VALIDATE_EXISTING_INSTANCE(warehouse);
		
		 String name = (String)warehouse.getAttribute(WAREHOUSE_FIELD_NAME);
		 String code = (String)warehouse.getAttribute(WAREHOUSE_FIELD_CODE);
		
		return UpdateWarehouse(uctx,warehouse.getId(),name,code );
	}
	
	@Export(ParameterNames={"name","code"})
	@TransactionProtect
	public Entity UpdateWarehouse(UserApplicationContext uctx,long warehouse_id,String name,String code ) throws WebApplicationException,PersistenceException
	{
		Entity caller 			= (Entity)uctx.getUser();
		Entity warehouse 			= GET(WAREHOUSE_ENTITY,warehouse_id);
		GUARD(caller, CAN_UPDATE_WAREHOUSE,GUARD_INSTANCE,warehouse,
		 	WAREHOUSE_FIELD_NAME,name,
		 	WAREHOUSE_FIELD_CODE,code
			);
		return updateWarehouse(warehouse,name,code );
	}

	public Entity updateWarehouse(Entity warehouse,String name,String code) throws WebApplicationException,PersistenceException
	{
		return doUpdate(warehouse,
		 	WAREHOUSE_FIELD_NAME,name,
		 	WAREHOUSE_FIELD_CODE,code
					  );
	}
	
	@Export
	@TransactionProtect
	public void DeleteWarehouse(UserApplicationContext uctx,long warehouse_id ) throws WebApplicationException,PersistenceException
	{
		Entity caller = (Entity)uctx.getUser();
		Entity warehouse = GET(WAREHOUSE_ENTITY,warehouse_id);
		GUARD(caller, CAN_DELETE_WAREHOUSE,GUARD_INSTANCE,warehouse);
		deleteWarehouse(warehouse);
	}
	
	public void deleteWarehouse(Entity warehouse)throws PersistenceException
	{
		DELETE(warehouse);
	}
	
	@Export
	public Entity GetWarehouse(UserApplicationContext uctx,long warehouse_id) throws WebApplicationException,PersistenceException
	{
		Entity caller = (Entity)uctx.getUser();
		Entity warehouse = GET(WAREHOUSE_ENTITY,warehouse_id);
		GUARD(caller, CAN_GET_WAREHOUSE,GUARD_INSTANCE,warehouse);
		return getWarehouse(warehouse_id);
	}
	
	public Entity getWarehouse(long warehouse_id) throws WebApplicationException,PersistenceException
	{
		return doGet("Warehouse",warehouse_id);
	}
	

	
	@Export
	public PagingQueryResult BrowseAllWarehouses(UserApplicationContext uctx, int offset, int page_size,String order_by, int order_by_order, boolean cache_results) throws WebApplicationException,PersistenceException
	{
		Entity caller 			= (Entity)uctx.getUser();
		GUARD(caller, CAN_BROWSE_WAREHOUSE,GUARD_BROWSE_INDEX,Query.PRIMARY_IDX,GUARD_BROWSE_OP,Query.EQ,GUARD_BROWSE_VALUE,Query.VAL_GLOB);				
		return doBrowseAll("Warehouse",offset,page_size,order_by,order_by_order,FILL_ALL_FIELDS,new String[]{"creator"},cache_results);
	}


	@Export
	public EntityDefinition GetWarehouseDefinition(UserApplicationContext uctx) throws WebApplicationException,PersistenceException
	{
		Entity caller 			= (Entity)uctx.getUser();
		GUARD(caller, CAN_GET_WAREHOUSE_DEFINITION, GUARD_TYPE,"Warehouse");				
		return doGetDefinition("Warehouse");
	}
	
	/* END WAREHOUSE	*/

	/* BEGIN PRODUCT */
	
	
	@Export(ParameterNames 	= {"product"})
	@TransactionProtect
	public Entity CreateProduct(UserApplicationContext uctx,Entity product) throws WebApplicationException,PersistenceException
	{
		VALIDATE_TYPE(PRODUCT_ENTITY, product);
		VALIDATE_NEW_INSTANCE(product);
		
		 String sku = (String)product.getAttribute(PRODUCT_FIELD_SKU);
		 String code = (String)product.getAttribute(PRODUCT_FIELD_CODE);
		 String description = (String)product.getAttribute(PRODUCT_FIELD_DESCRIPTION);
		 Entity fulfiller = (Entity)product.getAttribute(PRODUCT_FIELD_FULFILLER);
		
		return CreateProduct(uctx,sku,code,description,fulfiller);
	}
	
	@Export(ParameterNames={"sku","code","description","fulfiller"})
	@TransactionProtect
	public Entity CreateProduct(UserApplicationContext uctx,String sku,String code,String description,Entity fulfiller) throws WebApplicationException,PersistenceException
	{
		Entity caller = (Entity)uctx.getUser();
		GUARD(caller, CAN_CREATE_PRODUCT,GUARD_TYPE,PRODUCT_ENTITY,
		 	PRODUCT_FIELD_SKU,sku,
		 	PRODUCT_FIELD_CODE,code,
		 	PRODUCT_FIELD_DESCRIPTION,description,
		 	PRODUCT_FIELD_FULFILLER,fulfiller
					);
		
		return createProduct(caller,sku,code,description,fulfiller);

	}
	
	public Entity createProduct(Entity creator,String sku,String code,String description,Entity fulfiller) throws WebApplicationException,PersistenceException
	{

		Entity product =	doCreate(PRODUCT_ENTITY,
	    				creator,
		 	PRODUCT_FIELD_SKU,sku,
		 	PRODUCT_FIELD_CODE,code,
		 	PRODUCT_FIELD_DESCRIPTION,description,
		 	PRODUCT_FIELD_FULFILLER,fulfiller
		);
		return product;		
	}
	
	@Export(ParameterNames 	= {"product"})
	@TransactionProtect
	public Entity UpdateProduct(UserApplicationContext uctx,Entity product) throws WebApplicationException,PersistenceException
	{
		VALIDATE_TYPE(PRODUCT_ENTITY,product);
		VALIDATE_EXISTING_INSTANCE(product);
		
		 String sku = (String)product.getAttribute(PRODUCT_FIELD_SKU);
		 String code = (String)product.getAttribute(PRODUCT_FIELD_CODE);
		 String description = (String)product.getAttribute(PRODUCT_FIELD_DESCRIPTION);
		 Entity fulfiller = (Entity)product.getAttribute(PRODUCT_FIELD_FULFILLER);
		
		return UpdateProduct(uctx,product.getId(),sku,code,description,fulfiller );
	}
	
	@Export(ParameterNames={"sku","code","description","fulfiller"})
	@TransactionProtect
	public Entity UpdateProduct(UserApplicationContext uctx,long product_id,String sku,String code,String description,Entity fulfiller ) throws WebApplicationException,PersistenceException
	{
		Entity caller 			= (Entity)uctx.getUser();
		Entity product 			= GET(PRODUCT_ENTITY,product_id);
		GUARD(caller, CAN_UPDATE_PRODUCT,GUARD_INSTANCE,product,
		 	PRODUCT_FIELD_SKU,sku,
		 	PRODUCT_FIELD_CODE,code,
		 	PRODUCT_FIELD_DESCRIPTION,description,
		 	PRODUCT_FIELD_FULFILLER,fulfiller
			);
		return updateProduct(product,sku,code,description,fulfiller );
	}

	public Entity updateProduct(Entity product,String sku,String code,String description,Entity fulfiller) throws WebApplicationException,PersistenceException
	{
		return doUpdate(product,
		 	PRODUCT_FIELD_SKU,sku,
		 	PRODUCT_FIELD_CODE,code,
		 	PRODUCT_FIELD_DESCRIPTION,description,
		 	PRODUCT_FIELD_FULFILLER,fulfiller
					  );
	}
	
	@Export
	@TransactionProtect
	public void DeleteProduct(UserApplicationContext uctx,long product_id ) throws WebApplicationException,PersistenceException
	{
		Entity caller = (Entity)uctx.getUser();
		Entity product = GET(PRODUCT_ENTITY,product_id);
		GUARD(caller, CAN_DELETE_PRODUCT,GUARD_INSTANCE,product);
		deleteProduct(product);
	}
	
	public void deleteProduct(Entity product)throws PersistenceException
	{
		DELETE(product);
	}
	
	@Export
	public Entity GetProduct(UserApplicationContext uctx,long product_id) throws WebApplicationException,PersistenceException
	{
		Entity caller = (Entity)uctx.getUser();
		Entity product = GET(PRODUCT_ENTITY,product_id);
		GUARD(caller, CAN_GET_PRODUCT,GUARD_INSTANCE,product);
		return getProduct(product_id);
	}
	
	public Entity getProduct(long product_id) throws WebApplicationException,PersistenceException
	{
		return doGet("Product",product_id);
	}
	
	@Export
	public PagingQueryResult BrowseAllProducts(UserApplicationContext uctx, int offset, int page_size,String order_by, int order_by_order, boolean cache_results) throws WebApplicationException,PersistenceException
	{
		Entity caller 			= (Entity)uctx.getUser();
		GUARD(caller, CAN_BROWSE_PRODUCT,GUARD_BROWSE_INDEX,Query.PRIMARY_IDX,GUARD_BROWSE_OP,Query.EQ,GUARD_BROWSE_VALUE,Query.VAL_GLOB);				
		return doBrowseAll("Product",offset,page_size,order_by,order_by_order,FILL_ALL_FIELDS,new String[]{"creator"},cache_results);
	}


	@Export
	public EntityDefinition GetProductDefinition(UserApplicationContext uctx) throws WebApplicationException,PersistenceException
	{
		Entity caller 			= (Entity)uctx.getUser();
		GUARD(caller, CAN_GET_PRODUCT_DEFINITION, GUARD_TYPE,"Product");				
		return doGetDefinition("Product");
	}
	
	/* END PRODUCT	*/


	/////////
	
	public static final String WAREHOUSE_ENTITY 			= "Warehouse";
	public static final String WAREHOUSE_FIELD_NAME  		= "name";
	public static final String WAREHOUSE_FIELD_CODE  		= "code";

	
	public static final String PRODUCT_ENTITY 				= "Product";
	public static final String PRODUCT_FIELD_SKU 			= "sku";
	public static final String PRODUCT_FIELD_CODE 			= "code";
	public static final String PRODUCT_FIELD_NAME 			= "name";
	public static final String PRODUCT_FIELD_DESCRIPTION 	= "description";
	public static final String PRODUCT_FIELD_FULFILLER 		= "fulfiller";
	
	protected void defineEntities(Map<String,Object> config) throws PersistenceException,InitializationException
	{

		DEFINE_ENTITY
		(
				WAREHOUSE_ENTITY,
				WAREHOUSE_FIELD_NAME,Types.TYPE_STRING,"",
				WAREHOUSE_FIELD_CODE,Types.TYPE_STRING,""
		);

		DEFINE_ENTITY
		(
				PRODUCT_ENTITY,
				PRODUCT_FIELD_SKU,Types.TYPE_STRING,"",
				PRODUCT_FIELD_CODE,Types.TYPE_STRING,"",
				PRODUCT_FIELD_DESCRIPTION,Types.TYPE_TEXT,"",
				PRODUCT_FIELD_FULFILLER,Types.TYPE_REFERENCE,FieldDefinition.REF_TYPE_UNTYPED_ENTITY,null
		);
	
	}	
	
}
