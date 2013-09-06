/**
 * This file is part of the API for the Maniac Challenge 2013.
 *
 * The Maniac API is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Maniac API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package de.fu_berlin.maniac.exception;

/**
 * This exception occurs when you try to send a negative bid.
 * @author maniacchallenge
 *
 */
public class NegativeBidException extends ManiacException {

	private static final long serialVersionUID = 414756540571614673L;

	public NegativeBidException() {
		
		super(ThrowingComponent.Packets, "The bid must not be negative.");
		
	}
	
}
