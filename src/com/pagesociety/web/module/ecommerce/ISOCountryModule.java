package com.pagesociety.web.module.ecommerce;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jws.soap.SOAPBinding.Use;

import com.pagesociety.persistence.Entity;
import com.pagesociety.persistence.EntityIndex;
import com.pagesociety.persistence.PersistenceException;
import com.pagesociety.persistence.Query;
import com.pagesociety.persistence.QueryResult;
import com.pagesociety.persistence.Types;
import com.pagesociety.web.UserApplicationContext;
import com.pagesociety.web.WebApplication;
import com.pagesociety.web.exception.InitializationException;
import com.pagesociety.web.exception.WebApplicationException;
import com.pagesociety.web.module.Export;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.module.TransactionProtect;
import com.pagesociety.web.module.WebModule;
import com.pagesociety.web.module.WebStoreModule;
import com.pagesociety.web.module.ecommerce.gateway.BillingGatewayException;
import com.pagesociety.web.module.ecommerce.gateway.IBillingGateway;
import com.pagesociety.web.module.encryption.EncryptionModule;
import com.pagesociety.web.module.encryption.IEncryptionModule;


public class ISOCountryModule extends WebModule 
{
	
	
	public void init(WebApplication app, Map<String,Object> config) throws InitializationException
	{
		super.init(app,config);
		build_iso_3166_country_code_data();
	}
	
	private List<String> countries_list;
	private Map<String,String> country_to_code_map;
	private Map<String,String> code_to_country_map;
	
	@Export
	public List<String> GetCountries(UserApplicationContext uctx)
	{
		return getCountries();
	}
	
	public List<String> getCountries()
	{
		return countries_list;
	}
	
	
	@Export
	public String GetCountryCode(UserApplicationContext uctx,String country) throws WebApplicationException
	{
		String code =  getCountryCode(country);
		if(code == null)
			throw new WebApplicationException("BAD ISO 3166-1 COUNTRY NAME");
		return code;
	}
	
	public String getCountryCode(String country) 
	{
		return country_to_code_map.get(country);

	}
	
	@Export
	public String GetCountry(UserApplicationContext uctx,String code) throws WebApplicationException
	{
		String country = getCountry(code);
		if(country == null)
			throw new WebApplicationException("BAD ISO 3166-1 COUNTRY CODE");
		return country;
	}
	
	public String getCountry(String code)
	{
		return code_to_country_map.get(code);	
	}
	
	public void build_iso_3166_country_code_data()
	{
	String[] COUNTRY_CODES_LIST = new String[]{
						"004", "Afghanistan",
						"008", "Albania",
						"010", "Antarctica",
						"012", "Algeria",
						"016", "American Samoa",
						"020", "Andorra",
						"024", "Angola",
						"028", "Antigua and Barbuda",
						"031", "Azerbaijan",
						"032", "Argentina",
						"036", "Australia",
						"040", "Austria",
						"044", "Bahamas",
						"048", "Bahrdi",
						"112", "Belain",
						"050", "Bangladesh",
						"052", "Barbados",
						"056", "Belgium",
						"060", "Bermuda",
						"064", "Bhutan",
						"068", "Bolivia, Plurinational State of",
						"070", "Bosnia and Herzegovina",
						"072", "Botswana",
						"074", "Bouvet Island",
						"076", "Brazil",
						"084", "Belize",
						"086", "British Indian Ocean Territory",
						"090", "Solomon Islands",
						"092", "Virgin Islands, British",
						"096", "Brunei Darussalam",
						"100", "Bulgaria",
						"104", "Myanmar",
						"108", "Burunarus",
						"116", "Cambodia",
						"120", "Cameroon",
						"124", "Canada",
						"132", "Cape Verde",
						"136", "Cayman Islands",
						"140", "Central African Republic",
						"144", "Sri Lanka",
						"148", "Chad",
						"152", "Chile",
						"156", "China",
						"158", "Taiwan, Province of China",
						"162", "Christmas Island",
						"166", "Cocos (Keeling) Islands",
						"170", "Colombia",
						"174", "Comoros",
						"175", "Mayotte",
						"178", "Congo",
						"180", "Congo, the Democratic Republic of the",
						"184", "Cook Islands",
						"188", "Costa Rica",
						"191", "Croatia",
						"192", "Cuba",
						"196", "Cyprus",
						"203", "Czech Republic",
						"204", "Benin",
						"208", "Denmark",
						"212", "Dominica",
						"214", "Dominican Republic",
						"218", "Ecuador",
						"222", "El Salvador",
						"226", "Equatorial Guinea",
						"231", "Ethiopia",
						"232", "Eritrea",
						"233", "Estonia",
						"234", "Faroe Islands",
						"238", "Falkland Islands (Malvinas)",
						"239", "South Georgia and the South Sandwich Islands",
						"242", "Fiji",
						"246", "Finland",
						"248", "Åland Islands",
						"250", "France",
						"254", "French Guiana",
						"258", "French Polynesia",
						"260", "French Southern Territories",
						"262", "Djibouti",
						"266", "Gabon",
						"268", "Georgia",
						"270", "Gambia",
						"275", "Palestinian Territory, Occupied",
						"276", "Germany",
						"288", "Ghana",
						"292", "Gibraltar",
						"296", "Kiribati",
						"300", "Greece",
						"304", "Greenland",
						"308", "Grenada",
						"312", "Guadeloupe",
						"316", "Guam",
						"320", "Guatemala",
						"324", "Guinea",
						"328", "Guyana",
						"332", "Haiti",
						"334", "Heard Island and McDonald Islands",
						"336", "Holy See (Vatican City State)",
						"340", "Honduras",
						"344", "Hong Kong",
						"348", "Hungary",
						"352", "Iceland",
						"356", "India",
						"360", "Indonesia",
						"364", "Iran, Islamic Republic of",
						"368", "Iraq",
						"372", "Ireland",
						"376", "Israel",
						"380", "Italy",
						"384", "Côte d'Ivoire",
						"388", "Jamaica",
						"392", "Japan",
						"398", "Kazakhstan",
						"400", "Jordan",
						"404", "Kenya",
						"408", "Korea, Democratic People's Republic of",
						"410", "Korea, Republic of",
						"414", "Kuwait",
						"417", "Kyrgyzstan",
						"418", "Lao People's Democratic Republic",
						"422", "Lebanon",
						"426", "Lesotho",
						"428", "Latvia",
						"430", "Liberia",
						"434", "Libyan Arab Jamahiriya",
						"438", "Liechtenstein",
						"440", "Lithuania",
						"442", "Luxembourg",
						"446", "Macao",
						"450", "Madagascar",
						"454", "Malawi",
						"458", "Malaysia",
						"462", "Maldives",
						"466", "Mali",
						"470", "Malta",
						"474", "Martinique",
						"478", "Mauritania",
						"480", "Mauritius",
						"484", "Mexico",
						"492", "Monaco",
						"496", "Mongolia",
						"498", "Moldova, Republic of",
						"499", "Montenegro",
						"500", "Montserrat",
						"504", "Morocco",
						"508", "Mozambique",
						"512", "Oman",
						"516", "Namibia",
						"520", "Nauru",
						"524", "Nepal",
						"528", "Netherlands",
						"530", "Netherlands Antilles",
						"533", "Aruba",
						"540", "New Caledonia",
						"548", "Vanuatu",
						"554", "New Zealand",
						"558", "Nicaragua",
						"562", "Niger",
						"566", "Nigeria",
						"570", "Niue",
						"574", "Norfolk Island",
						"578", "Norway",
						"580", "Northern Mariana Islands",
						"581", "United States Minor Outlying Islands",
						"583", "Micronesia, Federated States of",
						"584", "Marshall Islands",
						"585", "Palau",
						"586", "Pakistan",
						"591", "Panama",
						"598", "Papua New Guinea",
						"600", "Paraguay",
						"604", "Peru",
						"608", "Philippines",
						"612", "Pitcairn",
						"616", "Poland",
						"620", "Portugal",
						"624", "Guinea-Bissau",
						"626", "Timor-Leste",
						"630", "Puerto Rico",
						"634", "Qatar",
						"638", "Réunion",
						"642", "Romania",
						"643", "Russian Federation",
						"646", "Rwanda",
						"652", "Saint Barthélemy",
						"654", "Saint Helena, Ascension and Tristan da Cunha",
						"659", "Saint Kitts and Nevis",
						"660", "Anguilla",
						"662", "Saint Lucia",
						"663", "Saint Martin (French part)",
						"666", "Saint Pierre and Miquelon",
						"670", "Saint Vincent and the Grenadines",
						"674", "San Marino",
						"678", "Sao Tome and Principe",
						"682", "Saudi Arabia",
						"686", "Senegal",
						"688", "Serbia",
						"690", "Seychelles",
						"694", "Sierra Leone",
						"702", "Singapore",
						"703", "Slovakia",
						"704", "Viet Nam",
						"705", "Slovenia",
						"706", "Somalia",
						"710", "South Africa",
						"716", "Zimbabwe",
						"724", "Spain",
						"732", "Western Sahara",
						"736", "Sudan",
						"740", "Suriname",
						"744", "Svalbard and Jan Mayen",
						"748", "Swaziland",
						"752", "Sweden",
						"756", "Switzerland",
						"760", "Syrian Arab Republic",
						"762", "Tajikistan",
						"764", "Thailand",
						"768", "Togo",
						"772", "Tokelau",
						"776", "Tonga",
						"780", "Trinidad and Tobago",
						"784", "United Arab Emirates",
						"788", "Tunisia",
						"792", "Turkey",
						"795", "Turkmenistan",
						"796", "Turks and Caicos Islands",
						"798", "Tuvalu",
						"800", "Uganda",
						"804", "Ukraine",
						"807", "Macedonia, the former Yugoslav Republic of",
						"818", "Egypt",
						"826", "United Kingdom",
						"831", "Guernsey",
						"832", "Jersey",
						"833", "Isle of Man",
						"834", "Tanzania, United Republic of",
						"840", "United States",
						"850", "Virgin Islands, U.S.",
						"854", "Burkina Faso",
						"858", "Uruguay",
						"860", "Uzbekistan",
						"862", "Venezuela, Bolivarian Republic of",
						"876", "Wallis and Futuna",
						"882", "Samoa",
						"887", "Yemen",
						"894", "Zambia"};
	
		countries_list = new ArrayList<String>();
		country_to_code_map = new HashMap<String,String>();
		code_to_country_map = new HashMap<String,String>();
		for(int i = 1;i < COUNTRY_CODES_LIST.length;i+=2)
		{
			String code 	= COUNTRY_CODES_LIST[i-1];
			String country 	= COUNTRY_CODES_LIST[i];
			countries_list.add(country);
			country_to_code_map.put(country,code);
			code_to_country_map.put(code,country);
		}
		
		
	}

}
