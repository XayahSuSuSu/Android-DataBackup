/* DO NOT EDIT! GENERATED AUTOMATICALLY! */
/* A substitute for POSIX 2008 <stddef.h>, for platforms that have issues.

   Copyright (C) 2009-2023 Free Software Foundation, Inc.

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

/* Written by Eric Blake.  */

/*
 * POSIX 2008 and ISO C 23 <stddef.h> for platforms that have issues.
 * <https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/stddef.h.html>
 */

#if __GNUC__ >= 3
#pragma GCC system_header
#endif


#if defined __need_wchar_t || defined __need_size_t  \
  || defined __need_ptrdiff_t || defined __need_NULL \
  || defined __need_wint_t
/* Special invocation convention inside gcc header files.  In
   particular, gcc provides a version of <stddef.h> that blindly
   redefines NULL even when __need_wint_t was defined, even though
   wint_t is not normally provided by <stddef.h>.  Hence, we must
   remember if special invocation has ever been used to obtain wint_t,
   in which case we need to clean up NULL yet again.  */

# if !(defined _GL_STDDEF_H && defined _GL_STDDEF_WINT_T)
#  ifdef __need_wint_t
#   define _GL_STDDEF_WINT_T
#  endif
#  include_next <stddef.h>
   /* On TinyCC, make sure that the macros that indicate the special invocation
      convention get undefined.  */
#  undef __need_wchar_t
#  undef __need_size_t
#  undef __need_ptrdiff_t
#  undef __need_NULL
#  undef __need_wint_t
# endif

#else
/* Normal invocation convention.  */

# ifndef _GL_STDDEF_H

/* On AIX 7.2, with xlc in 64-bit mode, <stddef.h> defines max_align_t to a
   type with alignment 4, but 'long' has alignment 8.  */
#  if defined _AIX && defined __LP64__
#   if !GNULIB_defined_max_align_t
#    ifdef _MAX_ALIGN_T
/* /usr/include/stddef.h has already defined max_align_t.  Override it.  */
typedef long rpl_max_align_t;
#     define max_align_t rpl_max_align_t
#    else
/* Prevent /usr/include/stddef.h from defining max_align_t.  */
typedef long max_align_t;
#     define _MAX_ALIGN_T
#    endif
#    define __CLANG_MAX_ALIGN_T_DEFINED
#    define GNULIB_defined_max_align_t 1
#   endif
#  endif

/* The include_next requires a split double-inclusion guard.  */

#  include_next <stddef.h>

/* On NetBSD 5.0, the definition of NULL lacks proper parentheses.  */
#  if (0 \
       && (!defined _GL_STDDEF_H || defined _GL_STDDEF_WINT_T))
#   undef NULL
#   ifdef __cplusplus
   /* ISO C++ says that the macro NULL must expand to an integer constant
      expression, hence '((void *) 0)' is not allowed in C++.  */
#    if __GNUG__ >= 3
    /* GNU C++ has a __null macro that behaves like an integer ('int' or
       'long') but has the same size as a pointer.  Use that, to avoid
       warnings.  */
#     define NULL __null
#    else
#     define NULL 0L
#    endif
#   else
#    define NULL ((void *) 0)
#   endif
#  endif

#  ifndef _GL_STDDEF_H
#   define _GL_STDDEF_H

/* This file uses _Noreturn.  */
#if !_GL_CONFIG_H_INCLUDED
 #error "Please include config.h first."
#endif

/* Some platforms lack wchar_t.  */
#if !1
# define wchar_t int
#endif

/* Some platforms lack max_align_t.  The check for _GCC_MAX_ALIGN_T is
   a hack in case the configure-time test was done with g++ even though
   we are currently compiling with gcc.
   On MSVC, max_align_t is defined only in C++ mode, after <cstddef> was
   included.  Its definition is good since it has an alignment of 8 (on x86
   and x86_64).
   Similarly on OS/2 kLIBC.  */
#if (defined _MSC_VER || (defined __KLIBC__ && !defined __LIBCN__)) \
    && defined __cplusplus
# include <cstddef>
#else
# if ! (1 || (defined _GCC_MAX_ALIGN_T && !defined __clang__))
#  if !GNULIB_defined_max_align_t
/* On the x86, the maximum storage alignment of double, long, etc. is 4,
   but GCC's C11 ABI for x86 says that max_align_t has an alignment of 8,
   and the C11 standard allows this.  Work around this problem by
   using __alignof__ (which returns 8 for double) rather than _Alignof
   (which returns 4), and align each union member accordingly.  */
#   if defined __GNUC__ || (__clang_major__ >= 4)
#    define _GL_STDDEF_ALIGNAS(type) \
       __attribute__ ((__aligned__ (__alignof__ (type))))
#   else
#    define _GL_STDDEF_ALIGNAS(type) /* */
#   endif
typedef union
{
  char *__p _GL_STDDEF_ALIGNAS (char *);
  double __d _GL_STDDEF_ALIGNAS (double);
  long double __ld _GL_STDDEF_ALIGNAS (long double);
  long int __i _GL_STDDEF_ALIGNAS (long int);
} rpl_max_align_t;
#   define max_align_t rpl_max_align_t
#   define __CLANG_MAX_ALIGN_T_DEFINED
#   define GNULIB_defined_max_align_t 1
#  endif
# endif
#endif

/* ISO C 23 § 7.21.1 The unreachable macro  */
#ifndef unreachable

/* Code borrowed from verify.h.  */
# ifndef _GL_HAS_BUILTIN_UNREACHABLE
#  if defined __clang_major__ && __clang_major__ < 5
#   define _GL_HAS_BUILTIN_UNREACHABLE 0
#  elif 4 < __GNUC__ + (5 <= __GNUC_MINOR__)
#   define _GL_HAS_BUILTIN_UNREACHABLE 1
#  elif defined __has_builtin
#   define _GL_HAS_BUILTIN_UNREACHABLE __has_builtin (__builtin_unreachable)
#  else
#   define _GL_HAS_BUILTIN_UNREACHABLE 0
#  endif
# endif

# if _GL_HAS_BUILTIN_UNREACHABLE
#  define unreachable() __builtin_unreachable ()
# elif 1200 <= _MSC_VER
#  define unreachable() __assume (0)
# else
/* Declare abort(), without including <stdlib.h>.  */
extern
#  if defined __cplusplus
"C"
#  endif
_Noreturn
void abort (void)
#  if defined __cplusplus && (__GLIBC__ >= 2)
throw ()
#  endif
;
#  define unreachable() abort ()
# endif

#endif

#  endif /* _GL_STDDEF_H */
# endif /* _GL_STDDEF_H */
#endif /* __need_XXX */
