/* DO NOT EDIT! GENERATED AUTOMATICALLY! */
/* stdckdint.h -- checked integer arithmetic

   Copyright 2022-2023 Free Software Foundation, Inc.

   This program is free software: you can redistribute it and/or modify it
   under the terms of the GNU Lesser General Public License as published
   by the Free Software Foundation; either version 2.1 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public License
   along with this program.  If not, see <https://www.gnu.org/licenses/>.  */

#ifndef _GL_STDCKDINT_H
#define _GL_STDCKDINT_H

#include "intprops-internal.h"

/* Store into *R the low-order bits of A + B, A - B, A * B, respectively.
   Return 1 if the result overflows, 0 otherwise.
   A, B, and *R can have any integer type other than char, bool, a
   bit-precise integer type, or an enumeration type.

   These are like the standard macros introduced in C23, except that
   arguments should not have side effects.  */

#define ckd_add(r, a, b) ((bool) _GL_INT_ADD_WRAPV (a, b, r))
#define ckd_sub(r, a, b) ((bool) _GL_INT_SUBTRACT_WRAPV (a, b, r))
#define ckd_mul(r, a, b) ((bool) _GL_INT_MULTIPLY_WRAPV (a, b, r))

#endif /* _GL_STDCKDINT_H */
