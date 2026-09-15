/* DO NOT EDIT! GENERATED AUTOMATICALLY! */
/* A GNU-like <limits.h>.

   Copyright 2016-2023 Free Software Foundation, Inc.

   This file is free software: you can redistribute it and/or modify
   it under the terms of the GNU Lesser General Public License as
   published by the Free Software Foundation; either version 2.1 of the
   License, or (at your option) any later version.

   This file is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public License
   along with this program.  If not, see <https://www.gnu.org/licenses/>.  */

#if __GNUC__ >= 3
#pragma GCC system_header
#endif


#if defined _GL_ALREADY_INCLUDING_LIMITS_H
/* Special invocation convention:
   On Haiku/x86_64, we have a sequence of nested includes
   <limits.h> -> <syslimits.h> -> <limits.h>.
   In this situation, LONG_MAX and INT_MAX are not yet defined,
   therefore we should not attempt to define LONG_BIT.  */

#include_next <limits.h>

#else
/* Normal invocation convention.  */

#ifndef _GL_LIMITS_H

# define _GL_ALREADY_INCLUDING_LIMITS_H

/* The include_next requires a split double-inclusion guard.  */
# include_next <limits.h>

# undef _GL_ALREADY_INCLUDING_LIMITS_H

#ifndef _GL_LIMITS_H
#define _GL_LIMITS_H

#ifndef LLONG_MIN
# if defined LONG_LONG_MIN /* HP-UX 11.31 */
#  define LLONG_MIN LONG_LONG_MIN
# elif defined LONGLONG_MIN /* IRIX 6.5 */
#  define LLONG_MIN LONGLONG_MIN
# elif defined __GNUC__
#  define LLONG_MIN (- __LONG_LONG_MAX__ - 1LL)
# endif
#endif
#ifndef LLONG_MAX
# if defined LONG_LONG_MAX /* HP-UX 11.31 */
#  define LLONG_MAX LONG_LONG_MAX
# elif defined LONGLONG_MAX /* IRIX 6.5 */
#  define LLONG_MAX LONGLONG_MAX
# elif defined __GNUC__
#  define LLONG_MAX __LONG_LONG_MAX__
# endif
#endif
#ifndef ULLONG_MAX
# if defined ULONG_LONG_MAX /* HP-UX 11.31 */
#  define ULLONG_MAX ULONG_LONG_MAX
# elif defined ULONGLONG_MAX /* IRIX 6.5 */
#  define ULLONG_MAX ULONGLONG_MAX
# elif defined __GNUC__
#  define ULLONG_MAX (__LONG_LONG_MAX__ * 2ULL + 1ULL)
# endif
#endif

/* The number of usable bits in an unsigned or signed integer type
   with minimum value MIN and maximum value MAX, as an int expression
   suitable in #if.  Cover all known practical hosts.  This
   implementation exploits the fact that MAX is 1 less than a power of
   2, and merely counts the number of 1 bits in MAX; "COBn" means
   "count the number of 1 bits in the low-order n bits").  */
#define _GL_INTEGER_WIDTH(min, max) (((min) < 0) + _GL_COB128 (max))
#define _GL_COB128(n) (_GL_COB64 ((n) >> 31 >> 31 >> 2) + _GL_COB64 (n))
#define _GL_COB64(n) (_GL_COB32 ((n) >> 31 >> 1) + _GL_COB32 (n))
#define _GL_COB32(n) (_GL_COB16 ((n) >> 16) + _GL_COB16 (n))
#define _GL_COB16(n) (_GL_COB8 ((n) >> 8) + _GL_COB8 (n))
#define _GL_COB8(n) (_GL_COB4 ((n) >> 4) + _GL_COB4 (n))
#define _GL_COB4(n) (!!((n) & 8) + !!((n) & 4) + !!((n) & 2) + !!((n) & 1))

#ifndef WORD_BIT
/* Assume 'int' is 32 bits wide.  */
# define WORD_BIT 32
#endif
#ifndef LONG_BIT
/* Assume 'long' is 32 or 64 bits wide.  */
# if LONG_MAX == INT_MAX
#  define LONG_BIT 32
# else
#  define LONG_BIT 64
# endif
#endif

/* Assume no multibyte character is longer than 16 bytes.  */
#ifndef MB_LEN_MAX
# define MB_LEN_MAX 16
#endif

/* Macros specified by C23 and by ISO/IEC TS 18661-1:2014.  */

#if (! defined ULLONG_WIDTH                                             \
     && (defined _GNU_SOURCE || defined __STDC_WANT_IEC_60559_BFP_EXT__ \
         || (defined __STDC_VERSION__ && 201710 < __STDC_VERSION__)))
# define CHAR_WIDTH _GL_INTEGER_WIDTH (CHAR_MIN, CHAR_MAX)
# define SCHAR_WIDTH _GL_INTEGER_WIDTH (SCHAR_MIN, SCHAR_MAX)
# define UCHAR_WIDTH _GL_INTEGER_WIDTH (0, UCHAR_MAX)
# define SHRT_WIDTH _GL_INTEGER_WIDTH (SHRT_MIN, SHRT_MAX)
# define USHRT_WIDTH _GL_INTEGER_WIDTH (0, USHRT_MAX)
# define INT_WIDTH _GL_INTEGER_WIDTH (INT_MIN, INT_MAX)
# define UINT_WIDTH _GL_INTEGER_WIDTH (0, UINT_MAX)
# define LONG_WIDTH _GL_INTEGER_WIDTH (LONG_MIN, LONG_MAX)
# define ULONG_WIDTH _GL_INTEGER_WIDTH (0, ULONG_MAX)
# define LLONG_WIDTH _GL_INTEGER_WIDTH (LLONG_MIN, LLONG_MAX)
# define ULLONG_WIDTH _GL_INTEGER_WIDTH (0, ULLONG_MAX)
#endif

/* Macros specified by C23.  */

#if (defined _GNU_SOURCE \
     || (defined __STDC_VERSION__ && 201710 < __STDC_VERSION__))
# if ! defined BOOL_WIDTH
#  define BOOL_WIDTH 1
#  define BOOL_MAX 1
# elif ! defined BOOL_MAX
#  define BOOL_MAX ((((1U << (BOOL_WIDTH - 1)) - 1) << 1) + 1)
# endif
#endif

/* Macro specified by POSIX.  */

/* The maximum ssize_t value.  Although it might not be of ssize_t type
   as it should be, it's too much trouble to fix this minor detail.  */
#ifndef SSIZE_MAX
# ifdef _WIN64
#  define SSIZE_MAX LLONG_MAX
# else
#  define SSIZE_MAX LONG_MAX
# endif
#endif

#endif /* _GL_LIMITS_H */
#endif /* _GL_LIMITS_H */
#endif
