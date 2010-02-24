package com.pagesociety.web.page.component;

import java.util.List;

import com.pagesociety.persistence.Entity;
import com.pagesociety.web.module.PagingQueryResult;
import com.pagesociety.web.page.Component;
import com.pagesociety.web.page.Container;

public class PagingSortedTable extends Component
{
	private PagingQueryResult results;
	private String[] visible_columns;

	public PagingSortedTable(Container parent, String id, String class_name)
	{
		super(parent, id, class_name);
	}

	public void setResults(PagingQueryResult results)
	{
		this.results = results;
	}

	public void setVisibleColumns(String... columns)
	{
		this.visible_columns = columns;
	}

	public void render(StringBuilder b)
	{
		if (results == null || visible_columns == null)
			return;
		b.append("<table ");
		render_attributes_id_class_style(b);
		b.append(">\n");
		b.append("<tr>\n");
		add_col_head(b, "id");
		for (int j = 0; j < visible_columns.length; j++)
			add_col_head(b, visible_columns[j]);
		b.append("</tr>\n");
		int s = results.size();
		List<Entity> es = results.getEntities();
		for (int i = 0; i < s; i++)
		{
			b.append("<tr>\n");
			Entity e = es.get(i);
			add_col(b, e.getId());
			for (int j = 0; j < visible_columns.length; j++)
				add_col(b, e.getAttribute(visible_columns[j]));
			b.append("</tr>\n");
		}
		b.append("</table>\n");
	}

	private void add_col(StringBuilder b, Object v)
	{
		b.append("  <td>");
		b.append(v);
		b.append("</td>\n");
	}

	private void add_col_head(StringBuilder b, Object v)
	{
		b.append("  <th>");
		b.append(v);
		b.append("</th>\n");
	}
}
