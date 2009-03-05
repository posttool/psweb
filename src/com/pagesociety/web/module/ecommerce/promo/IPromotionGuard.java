package com.pagesociety.web.module.ecommerce.promo;

import com.pagesociety.persistence.Entity;

public interface IPromotionGuard {

	public boolean canCreatePromotion(Entity user);

	public boolean canUpdatePromotion(Entity user, Entity promotion);

	public boolean canDeletePromotion(Entity user, Entity promotion);

	public boolean canCreateCouponPromotion(Entity user);

	public boolean canUpdateCouponPromotion(Entity user, Entity user_promotion);

	public boolean canDeleteCouponPromotion(Entity user, Entity coupon_promotion);
	
	public boolean canCreateGlobalPromotion(Entity user);

	public boolean canUpdateGlobalPromotion(Entity user, Entity global_promotion);

	public boolean canDeleteGlobalPromotion(Entity user, Entity global_promotion);

	public boolean canGetGlobalPromotions(Entity user);
	
	public boolean canGetCouponPromotions(Entity user);
	public boolean canGetPromotions(Entity user);

}
