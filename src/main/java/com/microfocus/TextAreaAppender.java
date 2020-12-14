package com.microfocus;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
	

public class TextAreaAppender extends AppenderBase<ILoggingEvent>
{
	public static Text textArea;

	public TextAreaAppender() {
	}

	@Override
	protected void append(final ILoggingEvent eventObject)
	{
		if(textArea != null) {
			Display.getDefault().asyncExec(() -> { 


				// If the text area already has lines in it, append a newline first.
				if (textArea.getText().length() > 0)
				{
					textArea.append("\n\n" + eventObject.getFormattedMessage());
				}
				else
				{
					textArea.setText(eventObject.getFormattedMessage());
				}
			});
		}
	}    
}