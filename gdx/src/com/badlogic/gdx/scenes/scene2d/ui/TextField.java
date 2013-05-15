/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

/** A single-line text input field.
 * <p>
 * The preferred height of a text field is the height of the {@link TextFieldStyle#font} and {@link TextFieldStyle#background}.
 * The preferred width of a text field is 150, a relatively arbitrary size.
 * <p>
 * The text field will copy the currently selected text when ctrl+c is pressed, and paste any text in the clipboard when ctrl+v is
 * pressed. Clipboard functionality is provided via the {@link Clipboard} interface. Currently there are two standard
 * implementations, one for the desktop and one for Android. The Android clipboard is a stub, as copy & pasting on Android is not
 * supported yet.
 * <p>
 * The text field allows you to specify an {@link OnscreenKeyboard} for displaying a softkeyboard and piping all key events
 * generated by the keyboard to the text field. There are two standard implementations, one for the desktop and one for Android.
 * The desktop keyboard is a stub, as a softkeyboard is not needed on the desktop. The Android {@link OnscreenKeyboard}
 * implementation will bring up the default IME.
 * @author mzechner
 * @author Nathan Sweet */
public class TextField extends Widget {
	static private final char BACKSPACE = 8;
	static private final char ENTER_DESKTOP = '\r';
	static private final char ENTER_ANDROID = '\n';
	static private final char TAB = '\t';
	static private final char DELETE = 127;
	static private final char BULLET = 149;

	static private final Vector2 tmp1 = new Vector2();
	static private final Vector2 tmp2 = new Vector2();
	static private final Vector2 tmp3 = new Vector2();

	TextFieldStyle style;
	String text, messageText;
	private CharSequence displayText;
	int cursor;
	private Clipboard clipboard;
	TextFieldListener listener;
	TextFieldFilter filter;
	OnscreenKeyboard keyboard = new DefaultOnscreenKeyboard();
	boolean focusTraversal = true;
	boolean disabled;
	boolean onlyFontChars = true;

	private boolean passwordMode;
	private StringBuilder passwordBuffer;

	private final Rectangle fieldBounds = new Rectangle();
	private final TextBounds textBounds = new TextBounds();
	private final Rectangle scissor = new Rectangle();
	float renderOffset, textOffset;
	private int visibleTextStart, visibleTextEnd;
	private final FloatArray glyphAdvances = new FloatArray();
	final FloatArray glyphPositions = new FloatArray();

	boolean cursorOn = true;
	private float blinkTime = 0.32f;
	long lastBlink;

	boolean hasSelection;
	int selectionStart;
	private float selectionX, selectionWidth;

	private char passwordCharacter = BULLET;

	InputListener inputListener;
	KeyRepeatTask keyRepeatTask = new KeyRepeatTask();
	float keyRepeatInitialTime = 0.4f;
	float keyRepeatTime = 0.1f;
	boolean rightAligned;

	int maxLength = 0;

	public TextField (String text, Skin skin) {
		this(text, skin.get(TextFieldStyle.class));
	}

	public TextField (String text, Skin skin, String styleName) {
		this(text, skin.get(styleName, TextFieldStyle.class));
	}

	public TextField (String text, TextFieldStyle style) {
		setStyle(style);
		this.clipboard = Gdx.app.getClipboard();
		setText(text);
		setWidth(getPrefWidth());
		setHeight(getPrefHeight());
		initialize();
	}

	private void initialize () {
		addListener(inputListener = new ClickListener() {
			public void clicked (InputEvent event, float x, float y) {
				if (getTapCount() > 1) setSelection(0, text.length());
			}

			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				if (!super.touchDown(event, x, y, pointer, button)) return false;
				if (pointer == 0 && button != 0) return false;
				if (disabled) return true;
				clearSelection();
				setCursorPosition(x);
				selectionStart = cursor;
				Stage stage = getStage();
				if (stage != null) stage.setKeyboardFocus(TextField.this);
				keyboard.show(true);
				return true;
			}

			public void touchDragged (InputEvent event, float x, float y, int pointer) {
				super.touchDragged(event, x, y, pointer);
				lastBlink = 0;
				cursorOn = false;
				setCursorPosition(x);
				hasSelection = true;
			}

			private void setCursorPosition (float x) {
				lastBlink = 0;
				cursorOn = false;
				x -= renderOffset + textOffset;
				for (int i = 0; i < glyphPositions.size; i++) {
					if (glyphPositions.items[i] > x) {
						cursor = Math.max(0, i - 1);
						return;
					}
				}
				cursor = Math.max(0, glyphPositions.size - 1);
			}

			public boolean keyDown (InputEvent event, int keycode) {
				if (disabled) return false;

				final BitmapFont font = style.font;

				lastBlink = 0;
				cursorOn = false;

				Stage stage = getStage();
				if (stage != null && stage.getKeyboardFocus() == TextField.this) {
					boolean repeat = false;
					boolean ctrl = Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT);
					if (ctrl) {
						// paste
						if (keycode == Keys.V) {
							paste();
							return true;
						}
						// copy
						if (keycode == Keys.C || keycode == Keys.INSERT) {
							copy();
							return true;
						}
						// cut
						if (keycode == Keys.X || keycode == Keys.DEL) {
							cut();
							return true;
						}
					}
					if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT)) {
						// paste
						if (keycode == Keys.INSERT) paste();
						// cut
						if (keycode == Keys.FORWARD_DEL) {
							if (hasSelection) {
								copy();
								delete();
							}
						}
						// selection
						if (keycode == Keys.LEFT) {
							if (!hasSelection) {
								selectionStart = cursor;
								hasSelection = true;
							}
							while (--cursor > 0 && ctrl) {
								char c = text.charAt(cursor);
								if (c >= 'A' && c <= 'Z') continue;
								if (c >= 'a' && c <= 'z') continue;
								if (c >= '0' && c <= '9') continue;
								break;
							}
							repeat = true;
						}
						if (keycode == Keys.RIGHT) {
							if (!hasSelection) {
								selectionStart = cursor;
								hasSelection = true;
							}
							int length = text.length();
							while (++cursor < length && ctrl) {
								char c = text.charAt(cursor - 1);
								if (c >= 'A' && c <= 'Z') continue;
								if (c >= 'a' && c <= 'z') continue;
								if (c >= '0' && c <= '9') continue;
								break;
							}
							repeat = true;
						}
						if (keycode == Keys.HOME) {
							if (!hasSelection) {
								selectionStart = cursor;
								hasSelection = true;
							}
							cursor = 0;
						}
						if (keycode == Keys.END) {
							if (!hasSelection) {
								selectionStart = cursor;
								hasSelection = true;
							}
							cursor = text.length();
						}

						cursor = Math.max(0, cursor);
						cursor = Math.min(text.length(), cursor);
					} else {
						// cursor movement or other keys (kill selection)
						if (keycode == Keys.LEFT) {
							while (cursor-- > 1 && ctrl) {
								char c = text.charAt(cursor - 1);
								if (c >= 'A' && c <= 'Z') continue;
								if (c >= 'a' && c <= 'z') continue;
								if (c >= '0' && c <= '9') continue;
								break;
							}
							clearSelection();
							repeat = true;
						}
						if (keycode == Keys.RIGHT) {
							int length = text.length();
							while (++cursor < length && ctrl) {
								char c = text.charAt(cursor - 1);
								if (c >= 'A' && c <= 'Z') continue;
								if (c >= 'a' && c <= 'z') continue;
								if (c >= '0' && c <= '9') continue;
								break;
							}
							clearSelection();
							repeat = true;
						}
						if (keycode == Keys.HOME) {
							cursor = 0;
							clearSelection();
						}
						if (keycode == Keys.END) {
							cursor = text.length();
							clearSelection();
						}

						cursor = Math.max(0, cursor);
						cursor = Math.min(text.length(), cursor);
					}
					if (repeat && (!keyRepeatTask.isScheduled() || keyRepeatTask.keycode != keycode)) {
						keyRepeatTask.keycode = keycode;
						keyRepeatTask.cancel();
						Timer.schedule(keyRepeatTask, keyRepeatInitialTime, keyRepeatTime);
					}
					return true;
				}
				return false;
			}

			public boolean keyUp (InputEvent event, int keycode) {
				if (disabled) return false;
				keyRepeatTask.cancel();
				return true;
			}

			public boolean keyTyped (InputEvent event, char character) {
				if (disabled) return false;

				final BitmapFont font = style.font;

				Stage stage = getStage();
				if (stage != null && stage.getKeyboardFocus() == TextField.this) {
					if (character == BACKSPACE && (cursor > 0 || hasSelection)) {
						if (!hasSelection) {
							text = text.substring(0, cursor - 1) + text.substring(cursor);
							updateDisplayText();
							cursor--;
							renderOffset = 0;
						} else {
							delete();
						}
					}
					if (character == DELETE) {
						if (cursor < text.length() || hasSelection) {
							if (!hasSelection) {
								text = text.substring(0, cursor) + text.substring(cursor + 1);
								updateDisplayText();
							} else {
								delete();
							}
						}
						return true;
					}
					if (character != ENTER_DESKTOP && character != ENTER_ANDROID) {
						if (filter != null && !filter.acceptChar(TextField.this, character)) return true;
					}
					if ((character == TAB || character == ENTER_ANDROID) && focusTraversal)
						next(Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT));
					if (font.containsCharacter(character)) {
						if (maxLength > 0 && text.length() + 1 > maxLength) {
							return true;
						}
						if (!hasSelection) {
							text = text.substring(0, cursor) + character + text.substring(cursor, text.length());
							updateDisplayText();
							cursor++;
						} else {
							int minIndex = Math.min(cursor, selectionStart);
							int maxIndex = Math.max(cursor, selectionStart);

							text = (minIndex > 0 ? text.substring(0, minIndex) : "")
								+ (maxIndex < text.length() ? text.substring(maxIndex, text.length()) : "");
							cursor = minIndex;
							text = text.substring(0, cursor) + character + text.substring(cursor, text.length());
							updateDisplayText();
							cursor++;
							clearSelection();
						}
					}
					if (listener != null) listener.keyTyped(TextField.this, character);
					return true;
				} else
					return false;
			}
		});
	}

	public void setMaxLength (int maxLength) {
		this.maxLength = maxLength;
	}

	public int getMaxLength () {
		return this.maxLength;
	}

	/** When false, text set by {@link #setText(String)} may contain characters not in the font, a space will be displayed instead.
	 * When true (the default), characters not in the font are stripped by setText. Characters not in the font are always stripped
	 * when typed or pasted. */
	public void setOnlyFontChars (boolean onlyFontChars) {
		this.onlyFontChars = onlyFontChars;
	}

	public void setStyle (TextFieldStyle style) {
		if (style == null) throw new IllegalArgumentException("style cannot be null.");
		this.style = style;
		invalidateHierarchy();
	}

	/** Sets the password character for the text field. The character must be present in the {@link BitmapFont} */
	public void setPasswordCharacter (char passwordCharacter) {
		this.passwordCharacter = passwordCharacter;
		if (passwordMode) updateDisplayText();
	}

	/** Returns the text field's style. Modifying the returned style may not have an effect until {@link #setStyle(TextFieldStyle)}
	 * is called. */
	public TextFieldStyle getStyle () {
		return style;
	}

	private void calculateOffsets () {
		float visibleWidth = getWidth();
		if (style.background != null) visibleWidth -= style.background.getLeftWidth() + style.background.getRightWidth();

		// Check if the cursor has gone out the left or right side of the visible area and adjust renderoffset.
		float position = glyphPositions.get(cursor);
		float distance = position - Math.abs(renderOffset);
		if (distance <= 0) {
			if (cursor > 0)
				renderOffset = -glyphPositions.get(cursor - 1);
			else
				renderOffset = 0;
		} else if (distance > visibleWidth) {
			renderOffset -= distance - visibleWidth;
		}

		// calculate first visible char based on render offset
		visibleTextStart = 0;
		textOffset = 0;
		float start = Math.abs(renderOffset);
		int len = glyphPositions.size;
		float startPos = 0;
		for (int i = 0; i < len; i++) {
			if (glyphPositions.items[i] >= start) {
				visibleTextStart = i;
				startPos = glyphPositions.items[i];
				textOffset = startPos - start;
				break;
			}
		}

		// calculate last visible char based on visible width and render offset
		visibleTextEnd = Math.min(displayText.length(), cursor + 1);
		for (; visibleTextEnd <= displayText.length(); visibleTextEnd++) {
			if (glyphPositions.items[visibleTextEnd] - startPos > visibleWidth) break;
		}
		visibleTextEnd = Math.max(0, visibleTextEnd - 1);

		// calculate selection x position and width
		if (hasSelection) {
			int minIndex = Math.min(cursor, selectionStart);
			int maxIndex = Math.max(cursor, selectionStart);
			float minX = Math.max(glyphPositions.get(minIndex), startPos);
			float maxX = Math.min(glyphPositions.get(maxIndex), glyphPositions.get(visibleTextEnd));
			selectionX = minX;
			selectionWidth = maxX - minX;
		}

		if (rightAligned) {
			textOffset = visibleWidth - (glyphPositions.items[visibleTextEnd] - startPos);
			if (hasSelection) selectionX += textOffset;
		}
	}

	@Override
	public void draw (SpriteBatch batch, float parentAlpha) {

		Stage stage = getStage();
		boolean focused = stage != null && stage.getKeyboardFocus() == this;

		final BitmapFont font = style.font;
		final Color fontColor = (disabled && style.disabledFontColor != null) ? style.disabledFontColor
			: ((focused && style.focusedFontColor != null) ? style.focusedFontColor : style.fontColor);
		final Drawable selection = style.selection;
		final Drawable cursorPatch = style.cursor;
		final Drawable background = (disabled && style.disabledBackground != null) ? style.disabledBackground
			: ((focused && style.focusedBackground != null) ? style.focusedBackground : style.background);

		Color color = getColor();
		float x = getX();
		float y = getY();
		float width = getWidth();
		float height = getHeight();
		float textY = textBounds.height / 2 + font.getDescent();

		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
		float bgLeftWidth = 0;
		if (background != null) {
			background.draw(batch, x, y, width, height);
			bgLeftWidth = background.getLeftWidth();
			float bottom = background.getBottomHeight();
			textY = (int)(textY + (height - background.getTopHeight() - bottom) / 2 + bottom);
		} else
			textY = (int)(textY + height / 2);

		calculateOffsets();

		if (focused && hasSelection && selection != null) {
			selection.draw(batch, x + selectionX + bgLeftWidth + renderOffset, y + textY - textBounds.height - font.getDescent(),
				selectionWidth, textBounds.height + font.getDescent() / 2);
		}

		float yOffset = font.isFlipped() ? -textBounds.height : 0;
		if (displayText.length() == 0) {
			if (!focused && messageText != null) {
				if (style.messageFontColor != null) {
					font.setColor(style.messageFontColor.r, style.messageFontColor.g, style.messageFontColor.b,
						style.messageFontColor.a * parentAlpha);
				} else
					font.setColor(0.7f, 0.7f, 0.7f, parentAlpha);
				BitmapFont messageFont = style.messageFont != null ? style.messageFont : font;
				messageFont.draw(batch, messageText, x + bgLeftWidth, y + textY + yOffset);
			}
		} else {
			font.setColor(fontColor.r, fontColor.g, fontColor.b, fontColor.a * parentAlpha);
			font.draw(batch, displayText, x + bgLeftWidth + textOffset, y + textY + yOffset, visibleTextStart, visibleTextEnd);
		}
		if (focused && !disabled) {
			blink();
			if (cursorOn && cursorPatch != null) {
				cursorPatch.draw(batch, x + bgLeftWidth + textOffset + glyphPositions.get(cursor)
					- glyphPositions.items[visibleTextStart] - 1, y + textY - textBounds.height - font.getDescent(),
					cursorPatch.getMinWidth(), textBounds.height + font.getDescent() / 2);
			}
		}
	}

	void updateDisplayText () {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			buffer.append(style.font.containsCharacter(c) ? c : ' ');
		}
		String text = buffer.toString();

		if (passwordMode && style.font.containsCharacter(passwordCharacter)) {
			if (passwordBuffer == null) passwordBuffer = new StringBuilder(text.length());
			if (passwordBuffer.length() > text.length()) //
				passwordBuffer.setLength(text.length());
			else {
				for (int i = passwordBuffer.length(), n = text.length(); i < n; i++)
					passwordBuffer.append(passwordCharacter);
			}
			displayText = passwordBuffer;
		} else
			displayText = text;
		style.font.computeGlyphAdvancesAndPositions(displayText, glyphAdvances, glyphPositions);
		if (selectionStart > text.length()) selectionStart = text.length();
	}

	private void blink () {
		long time = TimeUtils.nanoTime();
		if ((time - lastBlink) / 1000000000.0f > blinkTime) {
			cursorOn = !cursorOn;
			lastBlink = time;
		}
	}

	/** Copies the contents of this TextField to the {@link Clipboard} implementation set on this TextField. */
	public void copy () {
		if (hasSelection) {
			int minIndex = Math.min(cursor, selectionStart);
			int maxIndex = Math.max(cursor, selectionStart);
			clipboard.setContents(text.substring(minIndex, maxIndex));
		}
	}

	/** Copies the selected contents of this TextField to the {@link Clipboard} implementation set on this TextField, then removes
	 * it. */
	public void cut () {
		if (hasSelection) {
			copy();
			delete();
		}
	}

	/** Pastes the content of the {@link Clipboard} implementation set on this Textfield to this TextField. */
	void paste () {
		String content = clipboard.getContents();
		if (content != null) {
			StringBuilder buffer = new StringBuilder();
			for (int i = 0; i < content.length(); i++) {
				if (maxLength > 0 && text.length() + buffer.length() + 1 > maxLength) break;
				char c = content.charAt(i);
				if (!style.font.containsCharacter(c)) continue;
				if (filter != null && !filter.acceptChar(this, c)) continue;
				buffer.append(c);
			}
			content = buffer.toString();

			if (!hasSelection) {
				text = text.substring(0, cursor) + content + text.substring(cursor, text.length());
				updateDisplayText();
				cursor += content.length();
			} else {
				int minIndex = Math.min(cursor, selectionStart);
				int maxIndex = Math.max(cursor, selectionStart);

				text = (minIndex > 0 ? text.substring(0, minIndex) : "")
					+ (maxIndex < text.length() ? text.substring(maxIndex, text.length()) : "");
				cursor = minIndex;
				text = text.substring(0, cursor) + content + text.substring(cursor, text.length());
				updateDisplayText();
				cursor = minIndex + content.length();
				clearSelection();
			}

		}
	}

	void delete () {
		int minIndex = Math.min(cursor, selectionStart);
		int maxIndex = Math.max(cursor, selectionStart);
		text = (minIndex > 0 ? text.substring(0, minIndex) : "")
			+ (maxIndex < text.length() ? text.substring(maxIndex, text.length()) : "");
		updateDisplayText();
		cursor = minIndex;
		clearSelection();
	}

	/** Focuses the next TextField. If none is found, the keyboard is hidden. Does nothing if the text field is not in a stage.
	 * @param up If true, the TextField with the same or next smallest y coordinate is found, else the next highest. */
	public void next (boolean up) {
		Stage stage = getStage();
		if (stage == null) return;
		getParent().localToStageCoordinates(tmp1.set(getX(), getY()));
		TextField textField = findNextTextField(stage.getActors(), null, tmp2, tmp1, up);
		if (textField == null) { // Try to wrap around.
			if (up)
				tmp1.set(Float.MIN_VALUE, Float.MIN_VALUE);
			else
				tmp1.set(Float.MAX_VALUE, Float.MAX_VALUE);
			textField = findNextTextField(getStage().getActors(), null, tmp2, tmp1, up);
		}
		if (textField != null)
			stage.setKeyboardFocus(textField);
		else
			Gdx.input.setOnscreenKeyboardVisible(false);
	}

	private TextField findNextTextField (Array<Actor> actors, TextField best, Vector2 bestCoords, Vector2 currentCoords, boolean up) {
		for (int i = 0, n = actors.size; i < n; i++) {
			Actor actor = actors.get(i);
			if (actor == this) continue;
			if (actor instanceof TextField) {
				Vector2 actorCoords = actor.getParent().localToStageCoordinates(tmp3.set(actor.getX(), actor.getY()));
				if ((actorCoords.y < currentCoords.y || (actorCoords.y == currentCoords.y && actorCoords.x > currentCoords.x)) ^ up) {
					if (best == null
						|| (actorCoords.y > bestCoords.y || (actorCoords.y == bestCoords.y && actorCoords.x < bestCoords.x)) ^ up) {
						best = (TextField)actor;
						bestCoords.set(actorCoords);
					}
				}
			}
			if (actor instanceof Group) best = findNextTextField(((Group)actor).getChildren(), best, bestCoords, currentCoords, up);
		}
		return best;
	}

	/** @param listener May be null. */
	public void setTextFieldListener (TextFieldListener listener) {
		this.listener = listener;
	}

	/** @param filter May be null. */
	public void setTextFieldFilter (TextFieldFilter filter) {
		this.filter = filter;
	}

	/** If true (the default), tab/shift+tab will move to the next text field. */
	public void setFocusTraversal (boolean focusTraversal) {
		this.focusTraversal = focusTraversal;
	}

	/** @return May be null. */
	public String getMessageText () {
		return messageText;
	}

	/** Sets the text that will be drawn in the text field if no text has been entered.
	 * @param messageText may be null. */
	public void setMessageText (String messageText) {
		this.messageText = messageText;
	}

	public void setText (String text) {
		if (text == null) throw new IllegalArgumentException("text cannot be null.");

		BitmapFont font = style.font;

		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			if (maxLength > 0 && buffer.length() + 1 > maxLength) break;
			char c = text.charAt(i);
			if (onlyFontChars && !style.font.containsCharacter(c)) continue;
			if (filter != null && !filter.acceptChar(this, c)) continue;
			buffer.append(c);
		}

		this.text = buffer.toString();
		updateDisplayText();
		cursor = 0;
		clearSelection();

		textBounds.set(font.getBounds(displayText));
		textBounds.height -= font.getDescent() * 2;
		font.computeGlyphAdvancesAndPositions(displayText, glyphAdvances, glyphPositions);
	}

	/** @return Never null, might be an empty string. */
	public String getText () {
		return text;
	}

	/** Sets the selected text. */
	public void setSelection (int selectionStart, int selectionEnd) {
		if (selectionStart < 0) throw new IllegalArgumentException("selectionStart must be >= 0");
		if (selectionEnd < 0) throw new IllegalArgumentException("selectionEnd must be >= 0");
		selectionStart = Math.min(text.length(), selectionStart);
		selectionEnd = Math.min(text.length(), selectionEnd);
		if (selectionEnd == selectionStart) {
			clearSelection();
			return;
		}
		if (selectionEnd < selectionStart) {
			int temp = selectionEnd;
			selectionEnd = selectionStart;
			selectionStart = temp;
		}

		hasSelection = true;
		this.selectionStart = selectionStart;
		cursor = selectionEnd;
	}

	public void selectAll () {
		setSelection(0, text.length());
	}

	public void clearSelection () {
		hasSelection = false;
	}

	/** Sets the cursor position and clears any selection. */
	public void setCursorPosition (int cursorPosition) {
		if (cursorPosition < 0) throw new IllegalArgumentException("cursorPosition must be >= 0");
		clearSelection();
		cursor = Math.min(cursorPosition, text.length());
	}

	public int getCursorPosition () {
		return cursor;
	}

	/** Default is an instance of {@link DefaultOnscreenKeyboard}. */
	public OnscreenKeyboard getOnscreenKeyboard () {
		return keyboard;
	}

	public void setOnscreenKeyboard (OnscreenKeyboard keyboard) {
		this.keyboard = keyboard;
	}

	public void setClipboard (Clipboard clipboard) {
		this.clipboard = clipboard;
	}

	public float getPrefWidth () {
		return 150;
	}

	public float getPrefHeight () {
		float prefHeight = textBounds.height;
		if (style.background != null) {
			prefHeight = Math.max(prefHeight + style.background.getBottomHeight() + style.background.getTopHeight(),
				style.background.getMinHeight());
		}
		return prefHeight;
	}

	public void setRightAligned (boolean rightAligned) {
		this.rightAligned = rightAligned;
	}

	/** If true, the text in this text field will be shown as bullet characters. The font must have character 149 or this will have
	 * no affect. */
	public void setPasswordMode (boolean passwordMode) {
		this.passwordMode = passwordMode;
		updateDisplayText();
	}

	public void setBlinkTime (float blinkTime) {
		this.blinkTime = blinkTime;
	}

	public void setDisabled (boolean disabled) {
		this.disabled = disabled;
	}

	public boolean isDisabled () {
		return disabled;
	}

	public boolean isPasswordMode () {
		return passwordMode;
	}

	public TextFieldFilter getTextFieldFilter () {
		return filter;
	}

	class KeyRepeatTask extends Task {
		int keycode;

		public void run () {
			inputListener.keyDown(null, keycode);
		}
	}

	/** Interface for listening to typed characters.
	 * @author mzechner */
	static public interface TextFieldListener {
		public void keyTyped (TextField textField, char key);
	}

	/** Interface for filtering characters entered into the text field.
	 * @author mzechner */
	static public interface TextFieldFilter {
		/** @param textField
		 * @param key
		 * @return whether to accept the character */
		public boolean acceptChar (TextField textField, char key);

		static public class DigitsOnlyFilter implements TextFieldFilter {
			@Override
			public boolean acceptChar (TextField textField, char key) {
				return Character.isDigit(key);
			}

		}
	}

	/** An interface for onscreen keyboards. Can invoke the default keyboard or render your own keyboard!
	 * @author mzechner */
	static public interface OnscreenKeyboard {
		public void show (boolean visible);
	}

	/** The default {@link OnscreenKeyboard} used by all {@link TextField} instances. Just uses
	 * {@link Input#setOnscreenKeyboardVisible(boolean)} as appropriate. Might overlap your actual rendering, so use with care!
	 * @author mzechner */
	static public class DefaultOnscreenKeyboard implements OnscreenKeyboard {
		@Override
		public void show (boolean visible) {
			Gdx.input.setOnscreenKeyboardVisible(visible);
		}
	}

	/** The style for a text field, see {@link TextField}.
	 * @author mzechner
	 * @author Nathan Sweet */
	static public class TextFieldStyle {
		public BitmapFont font;
		public Color fontColor, focusedFontColor, disabledFontColor;
		/** Optional. */
		public Drawable background, focusedBackground, disabledBackground, cursor, selection;
		/** Optional. */
		public BitmapFont messageFont;
		/** Optional. */
		public Color messageFontColor;

		public TextFieldStyle () {
		}

		public TextFieldStyle (BitmapFont font, Color fontColor, Drawable cursor, Drawable selection, Drawable background) {
			this.background = background;
			this.cursor = cursor;
			this.font = font;
			this.fontColor = fontColor;
			this.selection = selection;
		}

		public TextFieldStyle (TextFieldStyle style) {
			this.messageFont = style.messageFont;
			if (style.messageFontColor != null) this.messageFontColor = new Color(style.messageFontColor);
			this.background = style.background;
			this.focusedBackground = style.focusedBackground;
			this.disabledBackground = style.disabledBackground;
			this.cursor = style.cursor;
			this.font = style.font;
			if (style.fontColor != null) this.fontColor = new Color(style.fontColor);
			if (style.focusedFontColor != null) this.focusedFontColor = new Color(style.focusedFontColor);
			if (style.disabledFontColor != null) this.disabledFontColor = new Color(style.disabledFontColor);
			this.selection = style.selection;
		}
	}
}
