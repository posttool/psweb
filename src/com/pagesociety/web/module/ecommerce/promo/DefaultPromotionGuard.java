package com.pagesociety.web.module.ecommerce.promo;

import com.pagesociety.persistence.Entity;

public class DefaultPromotionGuard 
{
	public boolean canCreatePromotion(Entity user){return false;}

	public boolean canUpdatePromotion(Entity user, Entity promotion){return false;}

	public boolean canDeletePromotion(Entity user, Entity promotion){return false;}

	public boolean canCreateCouponPromotion(Entity user){return false;}

	public boolean canUpdateCouponPromotion(Entity user, Entity user_promotion){return false;}

	public boolean canDeleteCouponPromotion(Entity user, Entity coupon_promotion){return false;}
	
	public boolean canCreateGlobalPromotion(Entity user){return false;}

	public boolean canUpdateGlobalPromotion(Entity user, Entity global_promotion){return false;}

	public boolean canDeleteGlobalPromotion(Entity user, Entity global_promotion){return false;}

	public boolean canGetGlobalPromotions(Entity user){return false;}

	public boolean canGetCouponPromotions(Entity user){return false;}

}
