/**
 * Aptana Studio
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.html.validator;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.aptana.core.build.IProblem;
import com.aptana.core.build.Problem;
import com.aptana.core.build.RequiredBuildParticipant;
import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.StringUtil;
import com.aptana.editor.css.ICSSConstants;
import com.aptana.editor.html.HTMLPlugin;
import com.aptana.editor.html.IHTMLConstants;
import com.aptana.editor.js.IJSConstants;
import com.aptana.index.core.build.BuildContext;
import com.aptana.parsing.ast.IParseError;
import com.aptana.parsing.ast.IParseError.Severity;

/**
 * Attaches HTML Parser errors from our own parser to the build context.
 * 
 * @author cwilliams
 */
public class HTMLParseErrorValidator extends RequiredBuildParticipant
{

	public void buildFile(BuildContext context, IProgressMonitor monitor)
	{
		if (context == null)
		{
			return;
		}

		Map<String, List<IProblem>> problems = new HashMap<String, List<IProblem>>();
		problems.put(IHTMLConstants.CONTENT_TYPE_HTML, new ArrayList<IProblem>());
		problems.put(IJSConstants.CONTENT_TYPE_JS, new ArrayList<IProblem>());
		problems.put(ICSSConstants.CONTENT_TYPE_CSS, new ArrayList<IProblem>());

		try
		{
			String source = context.getContents();
			if (!StringUtil.isEmpty(source))
			{
				URI path = context.getURI();
				String sourcePath = path.toString();

				context.getAST(); // Ensure a parse has happened

				// Add parse errors...
				for (IParseError parseError : context.getParseErrors())
				{
					int severity = (parseError.getSeverity() == Severity.ERROR) ? IMarker.SEVERITY_ERROR
							: IMarker.SEVERITY_WARNING;
					int line = -1;
					if (source != null)
					{
						line = getLineNumber(parseError.getOffset(), source);
					}
					String language = parseError.getLangauge();
					List<IProblem> langProblems = problems.get(language);
					langProblems.add(new Problem(severity, parseError.getMessage(), parseError.getOffset(), parseError
							.getLength(), line, sourcePath));
					problems.put(language, langProblems);
				}
			}
		}
		catch (CoreException e)
		{
			IdeLog.logError(HTMLPlugin.getDefault(), "Failed to parse for HTML Parse Error Validation", e); //$NON-NLS-1$
		}

		context.putProblems(IHTMLConstants.HTML_PROBLEM, problems.get(IHTMLConstants.CONTENT_TYPE_HTML));
		context.putProblems(IJSConstants.JS_PROBLEM_MARKER_TYPE, problems.get(IJSConstants.CONTENT_TYPE_JS));
		context.putProblems(ICSSConstants.CSS_PROBLEM, problems.get(ICSSConstants.CONTENT_TYPE_CSS));
	}

	public void deleteFile(BuildContext context, IProgressMonitor monitor)
	{
		if (context == null)
		{
			return;
		}
		context.removeProblems(IHTMLConstants.HTML_PROBLEM);
		context.removeProblems(IJSConstants.JS_PROBLEM_MARKER_TYPE);
		context.removeProblems(ICSSConstants.CSS_PROBLEM);
	}
}
