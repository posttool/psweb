package com.pagesociety.web.module.email.list;

import java.util.Date;
import java.util.Map;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.WebStoreModule;

public class MailingListManager extends WebStoreModule
{
	Entity CreateMailingList(String name){ return null; }
	Entity DeleteMailingList(long mlid){ return null; }
	PagingQueryResult GetMailingLists(){ return null; }

	Entity AddListRecipient(long mlid, String email, String name){ return null; }
	Entity RemoveListRecipient(long mlid, String email){ return null; }
	PagingQueryResult GetListRecipients(long mlid, int offset, int pagesize){ return null; }

	Entity CreateNewMailMessage(String title, long template_id){ return null; }
	Entity DeleteMailMessage(long mid){ return null; }
	Entity ListMailMessages(){ return null; }
	Entity SetMailMessageTemplate(long template_id){ return null; }
	Entity SetMailMessageData(long mail_message_id, Map<String,String> data){ return null; }
	Entity TestMailMessage(){ return null; }
	Entity TestMailMessage(long mail_message_id, String to){ return null; }
	Entity SendMailMessage(long mail_message_id, long[] mailing_list_ids){ return null; }

	PagingQueryResult ListSentMessages(Date start, Date end, int offset, int pagesize){ return null; }
	PagingQueryResult ViewMailMessageStatistics(long mail_message_id){ return null; } // who viewed images, who clicked links

	PagingQueryResult CreateTemplate(String name, String description, String html_body, String plain_text_version){ return null; }
	PagingQueryResult DeleteTemplate(long template_id){ return null; }
	PagingQueryResult ListTemplates(){ return null; }
	Map<String,String> GetParamMetaDataForTemplate(long template_id){ return null; }

	//all the resource stuff, upload etc

}
