package com.pagesociety.web.module.ecommerce.promo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;


import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.util.RandomGUID;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.SyncException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.ecommerce.BillingGatewayException;


//WE EXPRESS PROMOTIONS AS JAVASCRIPT PROGRAMS//
//HERE ARE SOME EXAMPLES//
/*first 3 months free */

/*
var num_times_applied = MEM.GET_IR1();

if(num_times_applied == 0)
	order.setAttribute("initial_fee",0.0)

var recurring_sku = EXPAND(order.getAttribute("sku"));
if(num_times_applied < 3)
{
	sku.setAttribute("price",0.0);
	num_times_applied++;
	MEM.SET_IR1(num_times_applied);
	return true;
}
return false;
*/

/*lifetime free*/
/*
var recurring_sku = EXPAND(order.getAttribute("sku"));
order.setAttribute("initial_fee",0.0)
sku.setAttribute("price",0.0);
return true;
*/

/*20% off */
/*
var recurring_sku = EXPAND(order.getAttribute("sku"));
var price 		  = recurring_sku.getAttribute("price");
sku.setAttribute("price",0.20 * price);
return true;

*/


public class PromotionModule extends WebStoreModule 
{

	private static final String SLOT_PROMOTION_GUARD  		 = "promotion-guard"; 
	IPromotionGuard   	guard;

		
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);
		guard				= (IPromotionGuard)getSlot(SLOT_PROMOTION_GUARD);

	}

	protected void defineSlots()
	{
		super.defineSlots();
		DEFINE_SLOT(SLOT_PROMOTION_GUARD,IPromotionGuard.class,false,DefaultPromotionGuard.class);
	
	}
	
	/////////////////BEGIN  M O D U L E   F U N C T I O N S/////////////////////////////////////////
	///THE PROMOTION
	@Export
	public Entity CreatePromotion(UserApplicationContext uctx,Entity promotion) throws WebApplicationException,PersistenceException,BillingGatewayException
	{

		VALIDATE_TYPE(PROMOTION_ENTITY, promotion);
		VALIDATE_NEW_INSTANCE(promotion);


		return CreatePromotion(uctx,
							  (String)promotion.getAttribute(PROMOTION_FIELD_TITLE),
							  (String)promotion.getAttribute(PROMOTION_FIELD_DESCRIPTION),
							  (String)promotion.getAttribute(PROMOTION_FIELD_PROGRAM));
	
	}
	

	
	@Export
	public Entity CreatePromotion(UserApplicationContext uctx,
								  String title,
							      String description,
							      String program) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(guard.canCreatePromotion(user));

		return createPromotion(user,title,description,program);

	}
	


	public Entity createPromotion(Entity creator,String title,String description,String program) throws WebApplicationException,PersistenceException
	{
		validate_promotion_source(title,program);
		
		return NEW(PROMOTION_ENTITY,
				   creator,
				   PROMOTION_FIELD_TITLE,title,
				   PROMOTION_FIELD_DESCRIPTION,description,
				   PROMOTION_FIELD_GR1,0L,
				   PROMOTION_FIELD_GR2,0L);
	}

	@Export
	public Entity UpdatePromotion(UserApplicationContext uctx,Entity promotion) throws WebApplicationException,PersistenceException,BillingGatewayException
	{

		VALIDATE_TYPE(PROMOTION_ENTITY, promotion);
		VALIDATE_EXISTING_INSTANCE(promotion);


		return UpdatePromotion(uctx,
							   promotion.getId(),
							  (String)promotion.getAttribute(PROMOTION_FIELD_TITLE),
							  (String)promotion.getAttribute(PROMOTION_FIELD_DESCRIPTION),
							  (String)promotion.getAttribute(PROMOTION_FIELD_PROGRAM));
	
	}
	
	@Export
	public Entity UpdatePromotion(UserApplicationContext uctx,
								  long promotion_id,
								  String title,
							      String description,
							      String program) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		Entity user = (Entity)uctx.getUser();
		Entity promotion = GET(PROMOTION_ENTITY,promotion_id);
		GUARD(guard.canUpdatePromotion(user,promotion));
		long gr1 = (Long)promotion.getAttribute(PROMOTION_FIELD_GR1);
		long gr2 = (Long)promotion.getAttribute(PROMOTION_FIELD_GR2);

		return updatePromotion(promotion,title,description,program,gr1,gr2);
	
	}
	

	public Entity updatePromotion(Entity promotion,String title,String description,String program,long gr1,long gr2) throws WebApplicationException,PersistenceException
	{
		validate_promotion_source(title,program);
		return NEW(PROMOTION_ENTITY,
				   promotion,
				   PROMOTION_FIELD_TITLE,title,
				   PROMOTION_FIELD_DESCRIPTION,description,
				   PROMOTION_FIELD_PROGRAM,program);
	}

	@Export
	public Entity SetPromotionProgramRegisters(UserApplicationContext uctx,
								  				long promotion_id,
								  				long gr1,
								  				long gr2) throws WebApplicationException,PersistenceException,BillingGatewayException
	{
		Entity user = (Entity)uctx.getUser();
		Entity promotion = GET(PROMOTION_ENTITY,promotion_id);
		GUARD(guard.canUpdatePromotion(user, promotion));
		
		return UPDATE(promotion,
					PROMOTION_FIELD_GR1,gr1,
					PROMOTION_FIELD_GR2,gr2);
	
	}
	
	//BE CAREFUL CALLING THIS. LOTS OF THINGS REFERENCE THIS//
	//TODO: make sure that nothing refers to this promotion.
	//i think there is a macro to make this easier
	@Export
	public Entity DeletePromotion(UserApplicationContext uctx,long promotion_id) throws WebApplicationException,PersistenceException
	{
		Entity user 	 = (Entity)uctx.getUser();
		Entity promotion = GET(PROMOTION_ENTITY,promotion_id);	
		GUARD(guard.canDeletePromotion(user, promotion));
		return deletePromotion(promotion);	
	}
	
	public Entity deletePromotion(Entity promotion) throws PersistenceException
	{
		return DELETE(promotion);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	//USER PROMOTION
	@Export
	public Entity CreateCouponPromotion(UserApplicationContext uctx,Entity coupon_promotion) throws WebApplicationException,PersistenceException
	{

		VALIDATE_TYPE(COUPON_PROMOTION_ENTITY, coupon_promotion);
		VALIDATE_NEW_INSTANCE(coupon_promotion);

		Entity promotion = (Entity)coupon_promotion.getAttribute(COUPON_PROMOTION_FIELD_PROMOTION);
		if(promotion == null)
			throw new WebApplicationException("MUST PROVIDE PROMOTION TO CREATE PROMOTION INSTANCE");
		VALIDATE_TYPE(PROMOTION_INSTANCE_ENTITY, promotion);
		promotion = GET(PROMOTION_ENTITY,promotion.getId());
		
		String promotion_code = (String)coupon_promotion.getAttribute(COUPON_PROMOTION_FIELD_PROMO_CODE);
		Integer num_times_code_can_be_used = (Integer)coupon_promotion.getAttribute(COUPON_PROMOTION_NO_TIMES_CODE_CAN_BE_USED);
		
		if(num_times_code_can_be_used == null ||
		   num_times_code_can_be_used.equals(0))
			num_times_code_can_be_used = 1;
		
		return CreateCouponPromotion(uctx,
									promotion_code,
									promotion,
									num_times_code_can_be_used,
								   (Date)coupon_promotion.getAttribute(COUPON_PROMOTION_FIELD_EXPIRATION_DATE));
	
	}
	
	@Export//if promotionCode is null one will be generated for you. 
	public Entity CreateCouponPromotion(UserApplicationContext uctx,
									   	String promotion_code,
										Entity promotion,
										int num_times_code_can_be_used,
									   	Date expiration_date) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(guard.canCreateCouponPromotion(user));		
		return createCouponPromotion(user,promotion_code,num_times_code_can_be_used,promotion,expiration_date);
	
	}

	//pass null promotion code to have one generated//
	//pass null expiration date for never expiring//
	public Entity createCouponPromotion(Entity creator,String promotion_code,int num_times_can_be_used,Entity promotion,Date expiration_date) throws WebApplicationException,PersistenceException
	{
		if(promotion_code == null)
			promotion_code = RandomGUID.getGUID();
		else
		{
			QueryResult result = QUERY(getCouponPromotionByPromoCodeQ(promotion_code));
			if(result.size() != 0)
				throw new WebApplicationException("NON UNIQUE PROMO CODE: "+promotion_code+". PROMO WITH CODE "+promotion_code+" ALREADY EXISTS");			
		}
		
		return NEW(COUPON_PROMOTION_ENTITY,
					creator,
					COUPON_PROMOTION_FIELD_PROMO_CODE,promotion_code,
					COUPON_PROMOTION_NO_TIMES_CODE_CAN_BE_USED,num_times_can_be_used,
					COUPON_PROMOTION_NO_TIMES_CODE_HAS_BEEN_USED,0,
					COUPON_PROMOTION_FIELD_PROMOTION,promotion,
					COUPON_PROMOTION_FIELD_EXPIRATION_DATE,expiration_date);
	}

	@Export
	public Entity SetCouponPromotionExpirationDate(UserApplicationContext uctx,long coupon_promotion_id,Date expiration_date) throws WebApplicationException,PersistenceException
	{		
		Entity user = (Entity)uctx.getUser();
		Entity coupon_promotion = GET(COUPON_PROMOTION_ENTITY,coupon_promotion_id);
		GUARD(guard.canUpdateCouponPromotion(user,coupon_promotion));
		return setCouponPromotionExpirationDate(coupon_promotion, expiration_date);

	}

	public Entity setCouponPromotionExpirationDate(Entity user_promotion,Date expiration_date) throws PersistenceException
	{
		return UPDATE(user_promotion, COUPON_PROMOTION_FIELD_EXPIRATION_DATE, expiration_date);
	}


	@Export
	public Entity DeleteCouponPromotion(UserApplicationContext uctx,long coupon_promotion_id) throws WebApplicationException,PersistenceException
	{
		Entity user 	 		= (Entity)uctx.getUser();
		Entity coupon_promotion = GET(COUPON_PROMOTION_ENTITY,coupon_promotion_id);	
		GUARD(guard.canDeleteCouponPromotion(user, coupon_promotion));
		return deleteCouponPromotion(coupon_promotion);	
	}
	
	public Entity deleteCouponPromotion(Entity coupon_promotion) throws PersistenceException
	{
		return DELETE(coupon_promotion);
	}

	
	@Export
	public PagingQueryResult GetCouponPromotions(UserApplicationContext uctx,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		GUARD(guard.canGetCouponPromotions(user));		
		Query q = getCouponPromotionsQ();
		q.offset(offset);
		q.pageSize(page_size);
		q.orderBy(FIELD_DATE_CREATED,Query.DESC);
		return PAGING_QUERY(q);
	}
	
		
	public Query getCouponPromotionsQ()
	{
		Query q = new Query(COUPON_PROMOTION_ENTITY);
		q.idx(Query.PRIMARY_IDX);
		q.eq(Query.VAL_GLOB);
		return q;
	}
	
	public Entity createPromotionInstance(Entity creator,String origin,Entity promotion) throws WebApplicationException,PersistenceException
	{

		return NEW(PROMOTION_INSTANCE_ENTITY,
				   creator,
				   PROMOTION_INSTANCE_FIELD_ORIGIN,origin,
				   PROMOTION_INSTANCE_FIELD_PROMOTION,promotion,
				   PROMOTION_INSTANCE_FIELD_IR1,0L,
				   PROMOTION_INSTANCE_FIELD_IR2,0L,
				   PROMOTION_INSTANCE_FIELD_IR3,0L,	   
				   PROMOTION_INSTANCE_FIELD_IR4,0L,
				   PROMOTION_INSTANCE_FIELD_SR1,null,
				   PROMOTION_INSTANCE_FIELD_SR2,null,
				   PROMOTION_INSTANCE_FIELD_FPR1,0.0,
				   PROMOTION_INSTANCE_FIELD_FPR2,0.0);
	}		  
	
	

	public static int ERROR_EXPIRED_PROMO_CODE 		   = 0x01;
	public static int ERROR_BAD_PROMO_CODE 		       = 0x02;
	public static int ERROR_PROMO_CODE_USED_UP 		   = 0x03;
	@Export //CAN GET MULTIPLE PROMOS FOR THE SAME CODE...REGISTERS WILL REFLECT ALL ACTIVITY FOR THAT CODE I.E. ALL USERS
	//THIS SHOULD WORK FINE IF THE PROMOS ARE SIMPLE LIKE 20%OFF, 20%OFF FIRST SIX BILLING CYCLES SEEMS HARD BECAUSE
	//THE USER PROMOTION NEEDS PRIVATE SPACE TO KEEP TRACK OF HOW MANY TIMES BILLED
	public Entity GetCouponPromotionByPromoCode(UserApplicationContext uctx,String promo_code) throws WebApplicationException,PersistenceException
	{
		Entity user   = (Entity)uctx.getUser();
		if(promo_code == null)
			throw new WebApplicationException("NEED TO PROVIDE NOT NULL PROMO CODE");
		
		Query q = getCouponPromotionByPromoCodeQ(promo_code);
		QueryResult result = QUERY(q);
		if(result.size() == 0)
			throw new WebApplicationException("BAD PROMO CODE",ERROR_BAD_PROMO_CODE);
		if(result.size() > 1)
			LOG("MULTIPLE PROMOTIONS ["+result.getEntities()+"] FOR SINGLE PROMO CODE "+promo_code+" DATA INTEGRITY ISSUE.");
		
		Entity coupon_promo = result.getEntities().get(0);
		Date expr_date = (Date)coupon_promo.getAttribute(COUPON_PROMOTION_FIELD_EXPIRATION_DATE);
		Date now = new Date();
		if(now.getTime() > expr_date.getTime())
			throw new WebApplicationException("EXPIRED PROMOCODE "+promo_code,ERROR_EXPIRED_PROMO_CODE);
		
		int number_times_can_be_used = (Integer)coupon_promo.getAttribute(COUPON_PROMOTION_NO_TIMES_CODE_CAN_BE_USED);
		int number_times_has_been_used = (Integer)coupon_promo.getAttribute(COUPON_PROMOTION_NO_TIMES_CODE_HAS_BEEN_USED);
		if(number_times_has_been_used == number_times_can_be_used)
				throw new WebApplicationException("PROMOCODE USED UP "+promo_code,ERROR_PROMO_CODE_USED_UP);
		
		Entity	promo_instance = createPromotionInstance((Entity)uctx.getUser(), promo_code,(Entity)coupon_promo.getAttribute(COUPON_PROMOTION_FIELD_PROMOTION));

		UPDATE(coupon_promo,COUPON_PROMOTION_NO_TIMES_CODE_HAS_BEEN_USED,++number_times_has_been_used);
		return promo_instance;
	}
	
	
	public Query getCouponPromotionByPromoCodeQ(String promo_code)
	{
		Query q = new Query(PROMOTION_INSTANCE_ENTITY);
		q.idx(IDX_COUPON_PROMOTION_BY_PROMO_CODE);
		q.eq(promo_code);
		return q;
	}
	
	//////////////////////////////////////////////////////////////////////////////////
	//GLOBAL PROMOTION
	@Export
	public Entity CreateGlobalPromotion(UserApplicationContext uctx,Entity global_promotion) throws WebApplicationException,PersistenceException
	{

		VALIDATE_TYPE(GLOBAL_PROMOTION_ENTITY, global_promotion);
		VALIDATE_NEW_INSTANCE(global_promotion);

		Entity promotion = (Entity)global_promotion.getAttribute(GLOBAL_PROMOTION_FIELD_PROMOTION);
		if(promotion == null)
			throw new WebApplicationException("MUST PROVIDE PROMOTION TO CREATE PROMOTION INSTANCE");
		VALIDATE_TYPE(PROMOTION_ENTITY, promotion);
		promotion = GET(PROMOTION_ENTITY,promotion.getId());
		
		return CreateGlobalPromotion(uctx,
								     promotion,
								    (Integer)global_promotion.getAttribute(GLOBAL_PROMOTION_FIELD_ACTIVE));
	
	}
	
	@Export
	public Entity CreateGlobalPromotion(UserApplicationContext uctx,
									   	Entity promotion,
									   	int active) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();

		GUARD(guard.canCreateGlobalPromotion(user));
		
		return createGlobalPromotionInstance(user,promotion,active);
	
	}
	
	
	public Entity createGlobalPromotionInstance(Entity creator,Entity promotion,int active) throws WebApplicationException,PersistenceException
	{
		return NEW(GLOBAL_PROMOTION_ENTITY,
				   creator,
				   GLOBAL_PROMOTION_FIELD_PROMOTION,promotion,
				   GLOBAL_PROMOTION_FIELD_ACTIVE,active);
	}		  
	
	@Export
	public Entity ActivateGlobalPromotion(UserApplicationContext uctx,long global_promotion_id) throws WebApplicationException,PersistenceException
	{		
		Entity user = (Entity)uctx.getUser();
		Entity global_promotion = GET(GLOBAL_PROMOTION_ENTITY,global_promotion_id);
		GUARD(guard.canUpdateGlobalPromotion(user,global_promotion));
		return setGlobalPromotionState(global_promotion,GLOBAL_PROMOTION_STATE_ACTIVE);

	}

	@Export
	public Entity DectivateGlobalPromotion(UserApplicationContext uctx,long global_promotion_id) throws WebApplicationException,PersistenceException
	{		
		Entity user = (Entity)uctx.getUser();
		Entity global_promotion = GET(GLOBAL_PROMOTION_ENTITY,global_promotion_id);	
		GUARD(guard.canUpdateGlobalPromotion(user,global_promotion));
		return setGlobalPromotionState(global_promotion,GLOBAL_PROMOTION_STATE_INACTIVE);

	}

	public Entity setGlobalPromotionState(Entity global_promotion,int state) throws PersistenceException
	{
		return UPDATE(global_promotion, GLOBAL_PROMOTION_FIELD_ACTIVE, state);
	}
	
	
	@Export
	public Entity DeleteGlobalPromotion(UserApplicationContext uctx,long global_promotion_id) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();
		Entity global_promotion = GET(GLOBAL_PROMOTION_ENTITY,global_promotion_id);
		GUARD(guard.canDeleteGlobalPromotion(user,global_promotion));
		return deleteGlobalPromotion(global_promotion);	
	}
	
	public Entity deleteGlobalPromotion(Entity global_promotion) throws PersistenceException
	{
		return DELETE(global_promotion);
	}
	private static final String PROMO_CODE_GLOBAL = "GLOBAL";
	//exclude_these_instances are instances of user_promotion instance that might exist already on an order for instance//
	//just pass in null if you want to get all of them as instances no matter what. this relates to a recurring order.
	//you wouldnt want to keep attaching the same instances at every billing cycle normally so get them when you
	//create the order and then dont ask for them again
	public List<Entity> getActiveGlobalPromotionsAsInstances(List<Entity> exclude_these_instances) throws PersistenceException
	{
		List<Entity> current_global_promotions = getActiveGlobalPromotions();
		List<Entity> ret = new ArrayList<Entity>();
		outer:
		for(int i = 0;i < current_global_promotions.size();i++)
		{
			Entity global_promotion = current_global_promotions.get(i);
			if(exclude_these_instances != null)
			{
				Entity global_promo_promo = (Entity)global_promotion.getAttribute(GLOBAL_PROMOTION_FIELD_PROMOTION);
				for(int j = 0; j < exclude_these_instances.size();j++)
				{
					Entity existing_instance = EXPAND(exclude_these_instances.get(j));
					if(((Entity)existing_instance.getAttribute(PROMOTION_INSTANCE_FIELD_PROMOTION)).equals(global_promo_promo))
						continue outer;
				}
			}
			Entity promotion_instance = null;
			Entity promotion = (Entity)global_promotion.getAttribute(GLOBAL_PROMOTION_FIELD_PROMOTION);
			try{
				promotion_instance = createPromotionInstance((Entity)global_promotion.getAttribute(FIELD_CREATOR), "GLOBAL", promotion);
			}catch(WebApplicationException wae)
			{
				//this should never happen. webapplication exception is only thrown when promo code exists
				ERROR(wae);
			}
			ret.add(promotion_instance);
		}
		return ret;
	}
	
	@Export
	public PagingQueryResult GetGlobalPromotions(UserApplicationContext uctx,int offset,int page_size) throws WebApplicationException,PersistenceException
	{
		Entity user = (Entity)uctx.getUser();

		GUARD(guard.canGetGlobalPromotions(user));		
		Query q = getGlobalPromotionsByStateQ(null);
		q.offset(offset);
		q.pageSize(page_size);
		q.orderBy(FIELD_DATE_CREATED,Query.DESC);
		return PAGING_QUERY(q);
	}
	
	public List<Entity> getActiveGlobalPromotions() throws PersistenceException
	{
		Query q = getGlobalPromotionsByStateQ(GLOBAL_PROMOTION_STATE_ACTIVE);
		QueryResult result = QUERY(q);
		return result.getEntities();
	}
	
	public Query getGlobalPromotionsByStateQ(Integer state)
	{
		Query q = new Query(GLOBAL_PROMOTION_ENTITY);
		q.idx(IDX_GLOBAL_PROMOTION_BY_ACTIVE);
		if(state == null)
			q.eq(Query.VAL_GLOB);
		else
			q.eq(state);
		return q;
	}
	
	///////////////////////////////////////
	////javascript stuff
	
	//return a list of promotions which were applied//
	public static final String FIELD_PROMOTIONS = "promotions";
	public List<Entity> applyPromotions(Entity order) throws WebApplicationException
	{
		List<Entity> promotions         = (List<Entity>)order.getAttribute(FIELD_PROMOTIONS);
		List<Entity> applied_promotions = new ArrayList<Entity>(); 
		if(promotions == null)
			return applied_promotions;
		
		for(int i = 0;i < promotions.size();i++)
		{
			Entity promotion = promotions.get(i);
			try{
				Boolean ret = (Boolean)apply_promotion(order, promotion);
				if(ret.equals(Boolean.TRUE)|| ret.equals(null))
					applied_promotions.add(promotion);
			}catch(Exception e)
			{
				throw new WebApplicationException("FAILED APPLYING PROMOTION "+promotion.getId());
			}
		}
		return applied_promotions;
	}

	public static final String PROMO_PROGRAM_WRAPPER_HEADER = "function apply_promotion(order){\nwith(P){\n";
	public static final String PROMO_PROGRAM_WRAPPER_FOOTER = "\n}\n}\n";
	public static final String JS_ENGINE_NAME = "JavaScript";
	private void validate_promotion_source(String promotion_name,String source) throws WebApplicationException
	{
		ScriptEngineManager mgr = new ScriptEngineManager();
		StringBuilder buf = new StringBuilder();
		buf.append(PROMO_PROGRAM_WRAPPER_HEADER);
		buf.append(source );
		buf.append(PROMO_PROGRAM_WRAPPER_FOOTER);
		 
		ScriptEngine jsEngine = mgr.getEngineByName(JS_ENGINE_NAME);		 
		ScriptContext ctx = jsEngine.getContext();
		ctx.setAttribute(ScriptEngine.FILENAME ,promotion_name, ScriptContext.ENGINE_SCOPE);
		ctx.setAttribute("P", this,ScriptContext.ENGINE_SCOPE );
		ctx.setAttribute("MEM",new MEM() ,ScriptContext.ENGINE_SCOPE );
		
		//check syntax//
		try {
			jsEngine.eval(buf.toString());
		} catch (ScriptException ex)
		{
			throw new WebApplicationException("SYNTAX ERROR IN PROMOTIONS SCRIPT.\n"+ex.getMessage(),ex);
		}    
			  
	}
	
	private Object apply_promotion(Entity order,Entity user_promotion_instance) throws PersistenceException,WebApplicationException
	{
		ScriptEngineManager mgr = new ScriptEngineManager();
		Entity promotion = EXPAND((Entity)user_promotion_instance.getAttribute(PROMOTION_INSTANCE_FIELD_PROMOTION));
		String promotion_name = (String)promotion.getAttribute(PROMOTION_FIELD_TITLE);
		String source = (String)promotion.getAttribute(PROMOTION_FIELD_PROGRAM);
		
		
		StringBuilder buf = new StringBuilder();
		buf.append(PROMO_PROGRAM_WRAPPER_HEADER);
		buf.append(source );
		buf.append(PROMO_PROGRAM_WRAPPER_FOOTER);
		 
		MEM m = new MEM((Long)user_promotion_instance.getAttribute(PROMOTION_INSTANCE_FIELD_IR1),
						(Long)user_promotion_instance.getAttribute(PROMOTION_INSTANCE_FIELD_IR2),
						(Long)user_promotion_instance.getAttribute(PROMOTION_INSTANCE_FIELD_IR3),
						(Long)user_promotion_instance.getAttribute(PROMOTION_INSTANCE_FIELD_IR4),
						(String)user_promotion_instance.getAttribute(PROMOTION_INSTANCE_FIELD_SR1),
						(String)user_promotion_instance.getAttribute(PROMOTION_INSTANCE_FIELD_SR2),
						(Double)user_promotion_instance.getAttribute(PROMOTION_INSTANCE_FIELD_FPR1),
						(Double)user_promotion_instance.getAttribute(PROMOTION_INSTANCE_FIELD_FPR2),
						(Long)promotion.getAttribute(PROMOTION_FIELD_GR1),
						(Long)promotion.getAttribute(PROMOTION_FIELD_GR2));
		
		ScriptEngine jsEngine = mgr.getEngineByName(JS_ENGINE_NAME);		 
		ScriptContext ctx = jsEngine.getContext();
		ctx.setAttribute(ScriptEngine.FILENAME ,promotion_name,ScriptContext.ENGINE_SCOPE);
		ctx.setAttribute("P", this,ScriptContext.ENGINE_SCOPE );
		ctx.setAttribute("MEM",m ,ScriptContext.ENGINE_SCOPE );
		
		  //apply promotion//
		Object ret = null;  
		try {
			  Invocable inv = (Invocable)jsEngine;
			  ret   = inv.invokeFunction("apply_promotion", order);
		   } catch (Exception ex) {
		      ex.printStackTrace();
		  }    
	
		//update user registers   
		user_promotion_instance.setAttribute(PROMOTION_INSTANCE_FIELD_IR1,m.GET_R1());
		user_promotion_instance.setAttribute(PROMOTION_INSTANCE_FIELD_IR2,m.GET_R2());
		user_promotion_instance.setAttribute(PROMOTION_INSTANCE_FIELD_IR3,m.GET_R3());
		user_promotion_instance.setAttribute(PROMOTION_INSTANCE_FIELD_IR4,m.GET_R4());
		user_promotion_instance.setAttribute(PROMOTION_INSTANCE_FIELD_SR1,m.GET_SR1());
		user_promotion_instance.setAttribute(PROMOTION_INSTANCE_FIELD_SR2,m.GET_SR2());
		user_promotion_instance.setAttribute(PROMOTION_INSTANCE_FIELD_FPR1,m.GET_FPR1());
		user_promotion_instance.setAttribute(PROMOTION_INSTANCE_FIELD_FPR2,m.GET_FPR2());
		SAVE_ENTITY(user_promotion_instance);
		
		//update global registers
		promotion.setAttribute(PROMOTION_FIELD_GR1, m.GET_GR1());
		promotion.setAttribute(PROMOTION_FIELD_GR2, m.GET_GR2());
		SAVE_ENTITY(promotion);
		return ret;
	}
	
	
	class MEM
	{

		private long   lr1;
		private long   lr2;
		private long   lr3;
		private long   lr4;
		private String sr1;
		private String sr2;
		private double fpr1;
		private double fpr2;
		private long gr1;
		private long gr2;
		
		public MEM()
		{
			
		}
		
		
		public MEM(long lr1,long lr2,long lr3,long lr4,String sr1,String sr2,double fpr1,double fpr2,long gr1,long gr2)
		{
			this.lr1  = lr1;
			this.lr1  = lr2;
			this.lr1  = lr3;
			this.lr1  = lr4;
			this.sr1  = sr1;
			this.sr2  = sr2;
			this.fpr1 = fpr1;
			this.fpr2 = fpr2;
			this.gr1  = gr1;
			this.gr2  = gr2;
		}
		
		
		public long GET_R1()
		{
			return lr1;
		}
		
		public void SET_R1(long l)
		{
			lr1 = l;
		}
		
		public long GET_R2()
		{
			return lr2;
		}
		
		public void SET_R2(long l)
		{
			lr2 = l;
		}
		
		public long GET_R3()
		{
			return lr3;
		}
		
		public void SET_R3(long l)
		{
			lr3 = l;
		}
		
		public long GET_R4()
		{
			return lr4;
		}
		
		public void SET_R4(long l)
		{
			lr4 = l;
		}
		
		public String GET_SR1()
		{
			return sr1;
		}
		
		public void SET_SR1(String s)
		{
			sr1 = s;
		}
		
		public String GET_SR2()
		{
			return sr2;
		}
		
		public void SET_SR2(String s)
		{
			sr2 = s;
		}
		
		public double GET_FPR1()
		{
			return fpr1;
		}
		
		public void SET_FPR1(double d)
		{
			fpr1 = d;
		}
		
		public double GET_FPR2()
		{
			return fpr2;
		}
		
		public void SET_FPR2(double d)
		{
			fpr2 = d;
		}
	
		public long GET_GR1()
		{
			return gr1;
		}
		public void SET_GR1(long l)
		{
			gr1 = l;
		}
		
		public long GET_GR2()
		{
			return gr2;
		}
		public void SET_GR2(long l)
		{
			gr2 = l;
		}
	}
	
	
	
	////////////////////////////////////////
	
	/////////////////E N D  M O D U L E   F U N C T I O N S/////////////////////////////////////////
		
	public static String PROMOTION_ENTITY 							= "Promotion";
	public static String PROMOTION_FIELD_TITLE	     				= "title";
	public static String PROMOTION_FIELD_DESCRIPTION 				= "description";
	public static String PROMOTION_FIELD_PROGRAM   					= "program";
	public static String PROMOTION_FIELD_GR1   						= "gr1";
	public static String PROMOTION_FIELD_GR2   						= "gr2";
	
	public static String GLOBAL_PROMOTION_ENTITY 			= "GlobalPromotion";
	public static String GLOBAL_PROMOTION_FIELD_PROMOTION 	= "promotion";
	public static String GLOBAL_PROMOTION_FIELD_ACTIVE   	= "active";
	
	public static String COUPON_PROMOTION_ENTITY 						= "CouponPromotion";
	public static String COUPON_PROMOTION_FIELD_PROMOTION 				= "promotion";
	public static String COUPON_PROMOTION_FIELD_PROMO_CODE 				= "promo_code";
	public static String COUPON_PROMOTION_FIELD_EXPIRATION_DATE 		= "expiration_date";
	public static String COUPON_PROMOTION_NO_TIMES_CODE_CAN_BE_USED 	= "no_times_code_can_be_used";
	public static String COUPON_PROMOTION_NO_TIMES_CODE_HAS_BEEN_USED 	= "no_times_code_has_been_used";
	
	public static String PROMOTION_INSTANCE_ENTITY 				= "UserPromotion";
	public static String PROMOTION_INSTANCE_FIELD_ORIGIN 		= "origin";
	public static String PROMOTION_INSTANCE_FIELD_PROMOTION 	= "promotion";
	public static String PROMOTION_INSTANCE_FIELD_IR1 			= "ir1";
	public static String PROMOTION_INSTANCE_FIELD_IR2 			= "ir2";
	public static String PROMOTION_INSTANCE_FIELD_IR3 			= "ir3";
	public static String PROMOTION_INSTANCE_FIELD_IR4 			= "ir4";
	public static String PROMOTION_INSTANCE_FIELD_SR1 			= "sr1";
	public static String PROMOTION_INSTANCE_FIELD_SR2 			= "sr2";
	public static String PROMOTION_INSTANCE_FIELD_FPR1 			= "fpr1";
	public static String PROMOTION_INSTANCE_FIELD_FPR2 			= "fpr2";

	private static final int GLOBAL_PROMOTION_STATE_INACTIVE 	= 0x01;
	private static final int GLOBAL_PROMOTION_STATE_ACTIVE 		= 0x02;
	protected void defineEntities(Map<String,Object> config) throws PersistenceException,SyncException
	{
		DEFINE_ENTITY(PROMOTION_ENTITY,
					  PROMOTION_FIELD_TITLE,Types.TYPE_STRING,"",
					  PROMOTION_FIELD_DESCRIPTION,Types.TYPE_STRING,"",
					  PROMOTION_FIELD_PROGRAM,Types.TYPE_STRING,null,
					  PROMOTION_FIELD_GR1,Types.TYPE_LONG,0L,
					  PROMOTION_FIELD_GR2,Types.TYPE_LONG,0L);
	
		DEFINE_ENTITY(GLOBAL_PROMOTION_ENTITY,
			  	  	  GLOBAL_PROMOTION_FIELD_PROMOTION,Types.TYPE_REFERENCE,PROMOTION_ENTITY,null,
			  	  	  GLOBAL_PROMOTION_FIELD_ACTIVE,Types.TYPE_INT,GLOBAL_PROMOTION_STATE_INACTIVE);
		
		DEFINE_ENTITY(COUPON_PROMOTION_ENTITY,
					  COUPON_PROMOTION_FIELD_PROMOTION,Types.TYPE_REFERENCE,PROMOTION_ENTITY,null,
					  COUPON_PROMOTION_FIELD_PROMO_CODE,Types.TYPE_STRING,null, //ORIGIN//
					  COUPON_PROMOTION_NO_TIMES_CODE_CAN_BE_USED,Types.TYPE_INT,1,
					  COUPON_PROMOTION_NO_TIMES_CODE_HAS_BEEN_USED,Types.TYPE_INT,0,
					  COUPON_PROMOTION_FIELD_EXPIRATION_DATE,Types.TYPE_DATE,null);

		DEFINE_ENTITY(PROMOTION_INSTANCE_ENTITY,
					  PROMOTION_INSTANCE_FIELD_ORIGIN,Types.TYPE_STRING,null, //ORIGIN//
					  PROMOTION_INSTANCE_FIELD_PROMOTION,Types.TYPE_REFERENCE,PROMOTION_ENTITY,null,
					  PROMOTION_INSTANCE_FIELD_IR1,Types.TYPE_LONG,0L, 
					  PROMOTION_INSTANCE_FIELD_IR2,Types.TYPE_LONG,0L,  			
					  PROMOTION_INSTANCE_FIELD_IR3,Types.TYPE_LONG,0L, 			
					  PROMOTION_INSTANCE_FIELD_IR4,Types.TYPE_LONG,0L, 			
					  PROMOTION_INSTANCE_FIELD_SR1,Types.TYPE_STRING,null, 			
					  PROMOTION_INSTANCE_FIELD_SR2,Types.TYPE_STRING,null, 		
					  PROMOTION_INSTANCE_FIELD_FPR1,Types.TYPE_DOUBLE,0.0, 		
					  PROMOTION_INSTANCE_FIELD_FPR2,Types.TYPE_DOUBLE,0.0);  

	}

	public static final String IDX_COUPON_PROMOTION_BY_PROMO_CODE   = "byPromoCode";
	public static final String IDX_GLOBAL_PROMOTION_BY_ACTIVE     	= "byActive";
	
	protected void defineIndexes(Map<String,Object> config) throws PersistenceException,SyncException
	{
		DEFINE_ENTITY_INDEX(COUPON_PROMOTION_ENTITY,IDX_COUPON_PROMOTION_BY_PROMO_CODE , EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, COUPON_PROMOTION_FIELD_PROMO_CODE);
		DEFINE_ENTITY_INDEX(GLOBAL_PROMOTION_ENTITY,IDX_GLOBAL_PROMOTION_BY_ACTIVE , EntityIndex.TYPE_SIMPLE_SINGLE_FIELD_INDEX, GLOBAL_PROMOTION_FIELD_ACTIVE);
	}
}
