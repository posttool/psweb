package com.pagesociety.web.bean;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityDefinition;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.FieldDefinition;
import com.pagesociety.web.ErrorMessage;
import com.pagesociety.web.amf.AmfLong;
import com.pagesociety.web.upload.MultipartForm;
import com.pagesociety.web.upload.UploadProgressInfo;

public class Beans
{
	public static void initDefault()
	{
		BeanRegistry.register(ErrorMessage.class);
		BeanRegistry.register(Entity.class, new String[] { "isDirty" });
		BeanRegistry.register(EntityIndex.class);
		BeanRegistry.register(FieldDefinition.class, new String[] { "isArray", "baseType" });
		BeanRegistry.register(EntityDefinition.class);
		//BeanRegistry.register(EntityIndexDefinition.class, new String[] { "attributeTypesAsMap" });
		//BeanRegistry.register(BDBQueryResult.class);
		//
		BeanRegistry.register(MultipartForm.class, new String[] { "fileNames", "parameterNames", "parameterMap", "observerMap" });
		BeanRegistry.register(UploadProgressInfo.class);
		//
		BeanRegistry.register(AmfLong.class);
		
	}
}
