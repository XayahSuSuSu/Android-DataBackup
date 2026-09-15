/* DO NOT EDIT! GENERATED AUTOMATICALLY! */
/* <uchar.h> substitute - 16-bit and 32-bit wide character types.
   Copyright (C) 2019-2023 Free Software Foundation, Inc.

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

/* Written by Bruno Haible <bruno@clisp.org>, 2019.  */

/*
 * ISO C 23 <uchar.h> for platforms that lack it.
 */

#ifndef _GL_UCHAR_H
#define _GL_UCHAR_H

#if __GNUC__ >= 3
#pragma GCC system_header
#endif


#if 1
# if defined __HAIKU__
/* Work around <https://dev.haiku-os.org/ticket/17040>.  */
#  include <stdint.h>
# endif
/* On AIX 7.2 with xlclang++, /usr/include/uchar.h produces compilation errors
   because it contains typedef definitions of char16_t and char32_t, however
   char16_t and char32_t are keywords in this situation.  To work around it,
   define char16_t and char32_t as macros.  */
# if defined __cplusplus && defined _AIX && defined __ibmxl__ && defined __clang__
#  define char16_t gl_char16_t
#  define char32_t gl_char32_t
# endif
# include_next <uchar.h>
#endif

/* This file uses _GL_INLINE_HEADER_BEGIN, _GL_INLINE, _GL_BEGIN_C_LINKAGE,
   _GL_ATTRIBUTE_PURE, GNULIB_POSIXCHECK, HAVE_RAW_DECL_*.  */
#if !_GL_CONFIG_H_INCLUDED
 #error "Please include config.h first."
#endif

/* Get uint_least16_t, uint_least32_t.  */
#include <stdint.h>

/* Get mbstate_t, size_t.  */
#include <wchar.h>

/* For the inline functions.  */
#include <string.h>
#include <wctype.h>

/* The __attribute__ feature is available in gcc versions 2.5 and later.
   The attribute __pure__ was added in gcc 2.96.  */
#ifndef _GL_ATTRIBUTE_PURE
# if __GNUC__ > 2 || (__GNUC__ == 2 && __GNUC_MINOR__ >= 96) || defined __clang__
#  define _GL_ATTRIBUTE_PURE __attribute__ ((__pure__))
# else
#  define _GL_ATTRIBUTE_PURE /* empty */
# endif
#endif

/* The definitions of _GL_FUNCDECL_RPL etc. are copied here.  */
/* C++ compatible function declaration macros.
   Copyright (C) 2010-2023 Free Software Foundation, Inc.

   This program is free software: you can redistribute it and/or modify it
   under the terms of the GNU Lesser General Public License as published
   by the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public License
   along with this program.  If not, see <https://www.gnu.org/licenses/>.  */

#ifndef _GL_CXXDEFS_H
#define _GL_CXXDEFS_H

/* Begin/end the GNULIB_NAMESPACE namespace.  */
#if defined __cplusplus && defined GNULIB_NAMESPACE
# define _GL_BEGIN_NAMESPACE namespace GNULIB_NAMESPACE {
# define _GL_END_NAMESPACE }
#else
# define _GL_BEGIN_NAMESPACE
# define _GL_END_NAMESPACE
#endif

/* The three most frequent use cases of these macros are:

   * For providing a substitute for a function that is missing on some
     platforms, but is declared and works fine on the platforms on which
     it exists:

       #if @GNULIB_FOO@
       # if !@HAVE_FOO@
       _GL_FUNCDECL_SYS (foo, ...);
       # endif
       _GL_CXXALIAS_SYS (foo, ...);
       _GL_CXXALIASWARN (foo);
       #elif defined GNULIB_POSIXCHECK
       ...
       #endif

   * For providing a replacement for a function that exists on all platforms,
     but is broken/insufficient and needs to be replaced on some platforms:

       #if @GNULIB_FOO@
       # if @REPLACE_FOO@
       #  if !(defined __cplusplus && defined GNULIB_NAMESPACE)
       #   undef foo
       #   define foo rpl_foo
       #  endif
       _GL_FUNCDECL_RPL (foo, ...);
       _GL_CXXALIAS_RPL (foo, ...);
       # else
       _GL_CXXALIAS_SYS (foo, ...);
       # endif
       _GL_CXXALIASWARN (foo);
       #elif defined GNULIB_POSIXCHECK
       ...
       #endif

   * For providing a replacement for a function that exists on some platforms
     but is broken/insufficient and needs to be replaced on some of them and
     is additionally either missing or undeclared on some other platforms:

       #if @GNULIB_FOO@
       # if @REPLACE_FOO@
       #  if !(defined __cplusplus && defined GNULIB_NAMESPACE)
       #   undef foo
       #   define foo rpl_foo
       #  endif
       _GL_FUNCDECL_RPL (foo, ...);
       _GL_CXXALIAS_RPL (foo, ...);
       # else
       #  if !@HAVE_FOO@   or   if !@HAVE_DECL_FOO@
       _GL_FUNCDECL_SYS (foo, ...);
       #  endif
       _GL_CXXALIAS_SYS (foo, ...);
       # endif
       _GL_CXXALIASWARN (foo);
       #elif defined GNULIB_POSIXCHECK
       ...
       #endif
*/

/* _GL_EXTERN_C declaration;
   performs the declaration with C linkage.  */
#if defined __cplusplus
# define _GL_EXTERN_C extern "C"
#else
# define _GL_EXTERN_C extern
#endif

/* _GL_FUNCDECL_RPL (func, rettype, parameters_and_attributes);
   declares a replacement function, named rpl_func, with the given prototype,
   consisting of return type, parameters, and attributes.
   Example:
     _GL_FUNCDECL_RPL (open, int, (const char *filename, int flags, ...)
                                  _GL_ARG_NONNULL ((1)));

   Note: Attributes, such as _GL_ATTRIBUTE_DEPRECATED, are supported in front
   of a _GL_FUNCDECL_RPL invocation only in C mode, not in C++ mode.  (That's
   because
     [[...]] extern "C" <declaration>;
   is invalid syntax in C++.)
 */
#define _GL_FUNCDECL_RPL(func,rettype,parameters_and_attributes) \
  _GL_FUNCDECL_RPL_1 (rpl_##func, rettype, parameters_and_attributes)
#define _GL_FUNCDECL_RPL_1(rpl_func,rettype,parameters_and_attributes) \
  _GL_EXTERN_C rettype rpl_func parameters_and_attributes

/* _GL_FUNCDECL_SYS (func, rettype, parameters_and_attributes);
   declares the system function, named func, with the given prototype,
   consisting of return type, parameters, and attributes.
   Example:
     _GL_FUNCDECL_SYS (open, int, (const char *filename, int flags, ...)
                                  _GL_ARG_NONNULL ((1)));
 */
#define _GL_FUNCDECL_SYS(func,rettype,parameters_and_attributes) \
  _GL_EXTERN_C rettype func parameters_and_attributes

/* _GL_CXXALIAS_RPL (func, rettype, parameters);
   declares a C++ alias called GNULIB_NAMESPACE::func
   that redirects to rpl_func, if GNULIB_NAMESPACE is defined.
   Example:
     _GL_CXXALIAS_RPL (open, int, (const char *filename, int flags, ...));

   Wrapping rpl_func in an object with an inline conversion operator
   avoids a reference to rpl_func unless GNULIB_NAMESPACE::func is
   actually used in the program.  */
#define _GL_CXXALIAS_RPL(func,rettype,parameters) \
  _GL_CXXALIAS_RPL_1 (func, rpl_##func, rettype, parameters)
#if defined __cplusplus && defined GNULIB_NAMESPACE
# define _GL_CXXALIAS_RPL_1(func,rpl_func,rettype,parameters) \
    namespace GNULIB_NAMESPACE                                \
    {                                                         \
      static const struct _gl_ ## func ## _wrapper            \
      {                                                       \
        typedef rettype (*type) parameters;                   \
                                                              \
        inline operator type () const                         \
        {                                                     \
          return ::rpl_func;                                  \
        }                                                     \
      } func = {};                                            \
    }                                                         \
    _GL_EXTERN_C int _gl_cxxalias_dummy
#else
# define _GL_CXXALIAS_RPL_1(func,rpl_func,rettype,parameters) \
    _GL_EXTERN_C int _gl_cxxalias_dummy
#endif

/* _GL_CXXALIAS_MDA (func, rettype, parameters);
   is to be used when func is a Microsoft deprecated alias, on native Windows.
   It declares a C++ alias called GNULIB_NAMESPACE::func
   that redirects to _func, if GNULIB_NAMESPACE is defined.
   Example:
     _GL_CXXALIAS_MDA (open, int, (const char *filename, int flags, ...));
 */
#define _GL_CXXALIAS_MDA(func,rettype,parameters) \
  _GL_CXXALIAS_RPL_1 (func, _##func, rettype, parameters)

/* _GL_CXXALIAS_RPL_CAST_1 (func, rpl_func, rettype, parameters);
   is like  _GL_CXXALIAS_RPL_1 (func, rpl_func, rettype, parameters);
   except that the C function rpl_func may have a slightly different
   declaration.  A cast is used to silence the "invalid conversion" error
   that would otherwise occur.  */
#if defined __cplusplus && defined GNULIB_NAMESPACE
# define _GL_CXXALIAS_RPL_CAST_1(func,rpl_func,rettype,parameters) \
    namespace GNULIB_NAMESPACE                                     \
    {                                                              \
      static const struct _gl_ ## func ## _wrapper                 \
      {                                                            \
        typedef rettype (*type) parameters;                        \
                                                                   \
        inline operator type () const                              \
        {                                                          \
          return reinterpret_cast<type>(::rpl_func);               \
        }                                                          \
      } func = {};                                                 \
    }                                                              \
    _GL_EXTERN_C int _gl_cxxalias_dummy
#else
# define _GL_CXXALIAS_RPL_CAST_1(func,rpl_func,rettype,parameters) \
    _GL_EXTERN_C int _gl_cxxalias_dummy
#endif

/* _GL_CXXALIAS_MDA_CAST (func, rettype, parameters);
   is like  _GL_CXXALIAS_MDA (func, rettype, parameters);
   except that the C function func may have a slightly different declaration.
   A cast is used to silence the "invalid conversion" error that would
   otherwise occur.  */
#define _GL_CXXALIAS_MDA_CAST(func,rettype,parameters) \
  _GL_CXXALIAS_RPL_CAST_1 (func, _##func, rettype, parameters)

/* _GL_CXXALIAS_SYS (func, rettype, parameters);
   declares a C++ alias called GNULIB_NAMESPACE::func
   that redirects to the system provided function func, if GNULIB_NAMESPACE
   is defined.
   Example:
     _GL_CXXALIAS_SYS (open, int, (const char *filename, int flags, ...));

   Wrapping func in an object with an inline conversion operator
   avoids a reference to func unless GNULIB_NAMESPACE::func is
   actually used in the program.  */
#if defined __cplusplus && defined GNULIB_NAMESPACE
# define _GL_CXXALIAS_SYS(func,rettype,parameters)            \
    namespace GNULIB_NAMESPACE                                \
    {                                                         \
      static const struct _gl_ ## func ## _wrapper            \
      {                                                       \
        typedef rettype (*type) parameters;                   \
                                                              \
        inline operator type () const                         \
        {                                                     \
          return ::func;                                      \
        }                                                     \
      } func = {};                                            \
    }                                                         \
    _GL_EXTERN_C int _gl_cxxalias_dummy
#else
# define _GL_CXXALIAS_SYS(func,rettype,parameters) \
    _GL_EXTERN_C int _gl_cxxalias_dummy
#endif

/* _GL_CXXALIAS_SYS_CAST (func, rettype, parameters);
   is like  _GL_CXXALIAS_SYS (func, rettype, parameters);
   except that the C function func may have a slightly different declaration.
   A cast is used to silence the "invalid conversion" error that would
   otherwise occur.  */
#if defined __cplusplus && defined GNULIB_NAMESPACE
# define _GL_CXXALIAS_SYS_CAST(func,rettype,parameters) \
    namespace GNULIB_NAMESPACE                          \
    {                                                   \
      static const struct _gl_ ## func ## _wrapper      \
      {                                                 \
        typedef rettype (*type) parameters;             \
                                                        \
        inline operator type () const                   \
        {                                               \
          return reinterpret_cast<type>(::func);        \
        }                                               \
      } func = {};                                      \
    }                                                   \
    _GL_EXTERN_C int _gl_cxxalias_dummy
#else
# define _GL_CXXALIAS_SYS_CAST(func,rettype,parameters) \
    _GL_EXTERN_C int _gl_cxxalias_dummy
#endif

/* _GL_CXXALIAS_SYS_CAST2 (func, rettype, parameters, rettype2, parameters2);
   is like  _GL_CXXALIAS_SYS (func, rettype, parameters);
   except that the C function is picked among a set of overloaded functions,
   namely the one with rettype2 and parameters2.  Two consecutive casts
   are used to silence the "cannot find a match" and "invalid conversion"
   errors that would otherwise occur.  */
#if defined __cplusplus && defined GNULIB_NAMESPACE
  /* The outer cast must be a reinterpret_cast.
     The inner cast: When the function is defined as a set of overloaded
     functions, it works as a static_cast<>, choosing the designated variant.
     When the function is defined as a single variant, it works as a
     reinterpret_cast<>. The parenthesized cast syntax works both ways.  */
# define _GL_CXXALIAS_SYS_CAST2(func,rettype,parameters,rettype2,parameters2) \
    namespace GNULIB_NAMESPACE                                                \
    {                                                                         \
      static const struct _gl_ ## func ## _wrapper                            \
      {                                                                       \
        typedef rettype (*type) parameters;                                   \
                                                                              \
        inline operator type () const                                         \
        {                                                                     \
          return reinterpret_cast<type>((rettype2 (*) parameters2)(::func));  \
        }                                                                     \
      } func = {};                                                            \
    }                                                                         \
    _GL_EXTERN_C int _gl_cxxalias_dummy
#else
# define _GL_CXXALIAS_SYS_CAST2(func,rettype,parameters,rettype2,parameters2) \
    _GL_EXTERN_C int _gl_cxxalias_dummy
#endif

/* _GL_CXXALIASWARN (func);
   causes a warning to be emitted when ::func is used but not when
   GNULIB_NAMESPACE::func is used.  func must be defined without overloaded
   variants.  */
#if defined __cplusplus && defined GNULIB_NAMESPACE
# define _GL_CXXALIASWARN(func) \
   _GL_CXXALIASWARN_1 (func, GNULIB_NAMESPACE)
# define _GL_CXXALIASWARN_1(func,namespace) \
   _GL_CXXALIASWARN_2 (func, namespace)
/* To work around GCC bug <https://gcc.gnu.org/bugzilla/show_bug.cgi?id=43881>,
   we enable the warning only when not optimizing.  */
# if !(defined __GNUC__ && !defined __clang__ && __OPTIMIZE__)
#  define _GL_CXXALIASWARN_2(func,namespace) \
    _GL_WARN_ON_USE (func, \
                     "The symbol ::" #func " refers to the system function. " \
                     "Use " #namespace "::" #func " instead.")
# elif __GNUC__ >= 3 && GNULIB_STRICT_CHECKING
#  define _GL_CXXALIASWARN_2(func,namespace) \
     extern __typeof__ (func) func
# else
#  define _GL_CXXALIASWARN_2(func,namespace) \
     _GL_EXTERN_C int _gl_cxxalias_dummy
# endif
#else
# define _GL_CXXALIASWARN(func) \
    _GL_EXTERN_C int _gl_cxxalias_dummy
#endif

/* _GL_CXXALIASWARN1 (func, rettype, parameters_and_attributes);
   causes a warning to be emitted when the given overloaded variant of ::func
   is used but not when GNULIB_NAMESPACE::func is used.  */
#if defined __cplusplus && defined GNULIB_NAMESPACE
# define _GL_CXXALIASWARN1(func,rettype,parameters_and_attributes) \
   _GL_CXXALIASWARN1_1 (func, rettype, parameters_and_attributes, \
                        GNULIB_NAMESPACE)
# define _GL_CXXALIASWARN1_1(func,rettype,parameters_and_attributes,namespace) \
   _GL_CXXALIASWARN1_2 (func, rettype, parameters_and_attributes, namespace)
/* To work around GCC bug <https://gcc.gnu.org/bugzilla/show_bug.cgi?id=43881>,
   we enable the warning only when not optimizing.  */
# if !(defined __GNUC__ && !defined __clang__ && __OPTIMIZE__)
#  define _GL_CXXALIASWARN1_2(func,rettype,parameters_and_attributes,namespace) \
    _GL_WARN_ON_USE_CXX (func, rettype, rettype, parameters_and_attributes, \
                         "The symbol ::" #func " refers to the system function. " \
                         "Use " #namespace "::" #func " instead.")
# else
#  define _GL_CXXALIASWARN1_2(func,rettype,parameters_and_attributes,namespace) \
     _GL_EXTERN_C int _gl_cxxalias_dummy
# endif
#else
# define _GL_CXXALIASWARN1(func,rettype,parameters_and_attributes) \
    _GL_EXTERN_C int _gl_cxxalias_dummy
#endif

#endif /* _GL_CXXDEFS_H */


_GL_INLINE_HEADER_BEGIN


#if !(1 || (defined __cplusplus && 0))

/* An 8-bit variant of wchar_t.
   Note: This type is only mandated by ISO C 23 or newer, and it does
   denote UTF-8 units.  */
typedef unsigned char char8_t;

#elif 1

typedef unsigned char gl_char8_t;
# define char8_t gl_char8_t

#endif

#if !(1 || (defined __cplusplus && 0))

/* A 16-bit variant of wchar_t.
   Note: This type is only mandated by ISO C 11 or newer.  In ISO C 23
   and newer, it denotes UTF-16 units; in older versions of ISO C it did
   so only on platforms on which __STDC_UTF_16__ was defined.  */
typedef uint_least16_t char16_t;

#elif 0

typedef uint_least16_t gl_char16_t;
# define char16_t gl_char16_t

#endif

#if !(1 || (defined __cplusplus && 0))

/* A 32-bit variant of wchar_t.
   Note: This type is only mandated by ISO C 11 or newer.  In ISO C 23
   and newer, it denotes UTF-32 code points; in older versions of ISO C
   it did so only on platforms on which __STDC_UTF_32__ was defined.
   In gnulib, we guarantee that it denotes UTF-32 code points if and
   only if the module 'uchar-c23' is in use.  */
typedef uint_least32_t char32_t;

#elif 0

typedef uint_least32_t gl_char32_t;
# define char32_t gl_char32_t

#endif

/* Define if a 'char32_t' can hold more characters than a 'wchar_t'.  */
#if 0                    /* 32-bit AIX, Cygwin, native Windows */
# define _GL_SMALL_WCHAR_T 1
#endif

/* Define if 'wchar_t', like 'char32_t',
     - is a 32-bit type, and
     - represents Unicode code points.
   For this test, we can use __STDC_ISO_10646__ (defined by glibc, musl libc,
   Cygwin) but need to consider _GL_SMALL_WCHAR_T, so as to exclude Cygwin.
   We cannot use __STDC_UTF_16__ or __STDC_UTF_32__
     - because these macros provide info about char16_t and char32_t (not
       wchar_t!), and
     - because GCC >= 4.9 defines these macros on all platforms, even on
       FreeBSD and Solaris.
   We should better not use __STD_UTF_16__, __STD_UTF_32__ either, because
   these macros are misspellings, only defined by Android's <uchar.h>.  */
#if defined __STDC_ISO_10646__ && !_GL_SMALL_WCHAR_T
/* glibc, musl libc */
# define _GL_WCHAR_T_IS_UCS4 1
#endif
#if _GL_WCHAR_T_IS_UCS4
static_assert (sizeof (char32_t) == sizeof (wchar_t));
#endif


/* Convert a single-byte character to a 32-bit wide character.  */
#if 0
# if _GL_WCHAR_T_IS_UCS4 && !defined IN_BTOC32
_GL_BEGIN_C_LINKAGE
_GL_INLINE _GL_ATTRIBUTE_PURE wint_t
btoc32 (int c)
{
  return btowc (c);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (btoc32, wint_t, (int c) _GL_ATTRIBUTE_PURE);
# endif
_GL_CXXALIAS_SYS (btoc32, wint_t, (int c));
_GL_CXXALIASWARN (btoc32);
#endif


/* Test a specific property of a 32-bit wide character.  */
#if 1
# if (_GL_WCHAR_T_IS_UCS4 && !GNULIB_defined_mbstate_t) && !defined IN_C32ISALNUM
_GL_BEGIN_C_LINKAGE
_GL_INLINE int
c32isalnum (wint_t wc)
{
  return iswalnum (wc);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32isalnum, int, (wint_t wc));
# endif
_GL_CXXALIAS_SYS (c32isalnum, int, (wint_t wc));
_GL_CXXALIASWARN (c32isalnum);
#endif
#if 1
# if (_GL_WCHAR_T_IS_UCS4 && !GNULIB_defined_mbstate_t) && !defined IN_C32ISALPHA
_GL_BEGIN_C_LINKAGE
_GL_INLINE int
c32isalpha (wint_t wc)
{
  return iswalpha (wc);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32isalpha, int, (wint_t wc));
# endif
_GL_CXXALIAS_SYS (c32isalpha, int, (wint_t wc));
_GL_CXXALIASWARN (c32isalpha);
#endif
#if 1
# if (_GL_WCHAR_T_IS_UCS4 && !GNULIB_defined_mbstate_t) && !defined IN_C32ISBLANK
_GL_BEGIN_C_LINKAGE
_GL_INLINE int
c32isblank (wint_t wc)
{
  return iswblank (wc);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32isblank, int, (wint_t wc));
# endif
_GL_CXXALIAS_SYS (c32isblank, int, (wint_t wc));
_GL_CXXALIASWARN (c32isblank);
#endif
#if 1
# if (_GL_WCHAR_T_IS_UCS4 && !GNULIB_defined_mbstate_t) && !defined IN_C32ISCNTRL
_GL_BEGIN_C_LINKAGE
_GL_INLINE int
c32iscntrl (wint_t wc)
{
  return iswcntrl (wc);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32iscntrl, int, (wint_t wc));
# endif
_GL_CXXALIAS_SYS (c32iscntrl, int, (wint_t wc));
_GL_CXXALIASWARN (c32iscntrl);
#endif
#if 1
# if (_GL_WCHAR_T_IS_UCS4 && !GNULIB_defined_mbstate_t) && !defined IN_C32ISDIGIT
_GL_BEGIN_C_LINKAGE
_GL_INLINE int
c32isdigit (wint_t wc)
{
  return iswdigit (wc);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32isdigit, int, (wint_t wc));
# endif
_GL_CXXALIAS_SYS (c32isdigit, int, (wint_t wc));
_GL_CXXALIASWARN (c32isdigit);
#endif
#if 1
# if (_GL_WCHAR_T_IS_UCS4 && !GNULIB_defined_mbstate_t) && !defined IN_C32ISGRAPH
_GL_BEGIN_C_LINKAGE
_GL_INLINE int
c32isgraph (wint_t wc)
{
  return iswgraph (wc);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32isgraph, int, (wint_t wc));
# endif
_GL_CXXALIAS_SYS (c32isgraph, int, (wint_t wc));
_GL_CXXALIASWARN (c32isgraph);
#endif
#if 1
# if (_GL_WCHAR_T_IS_UCS4 && !GNULIB_defined_mbstate_t) && !defined IN_C32ISLOWER
_GL_BEGIN_C_LINKAGE
_GL_INLINE int
c32islower (wint_t wc)
{
  return iswlower (wc);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32islower, int, (wint_t wc));
# endif
_GL_CXXALIAS_SYS (c32islower, int, (wint_t wc));
_GL_CXXALIASWARN (c32islower);
#endif
#if 1
# if (_GL_WCHAR_T_IS_UCS4 && !GNULIB_defined_mbstate_t) && !defined IN_C32ISPRINT
_GL_BEGIN_C_LINKAGE
_GL_INLINE int
c32isprint (wint_t wc)
{
  return iswprint (wc);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32isprint, int, (wint_t wc));
# endif
_GL_CXXALIAS_SYS (c32isprint, int, (wint_t wc));
_GL_CXXALIASWARN (c32isprint);
#endif
#if 1
# if (_GL_WCHAR_T_IS_UCS4 && !GNULIB_defined_mbstate_t) && !defined IN_C32ISPUNCT
_GL_BEGIN_C_LINKAGE
_GL_INLINE int
c32ispunct (wint_t wc)
{
  return iswpunct (wc);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32ispunct, int, (wint_t wc));
# endif
_GL_CXXALIAS_SYS (c32ispunct, int, (wint_t wc));
_GL_CXXALIASWARN (c32ispunct);
#endif
#if 1
# if (_GL_WCHAR_T_IS_UCS4 && !GNULIB_defined_mbstate_t) && !defined IN_C32ISSPACE
_GL_BEGIN_C_LINKAGE
_GL_INLINE int
c32isspace (wint_t wc)
{
  return iswspace (wc);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32isspace, int, (wint_t wc));
# endif
_GL_CXXALIAS_SYS (c32isspace, int, (wint_t wc));
_GL_CXXALIASWARN (c32isspace);
#endif
#if 1
# if (_GL_WCHAR_T_IS_UCS4 && !GNULIB_defined_mbstate_t) && !defined IN_C32ISUPPER
_GL_BEGIN_C_LINKAGE
_GL_INLINE int
c32isupper (wint_t wc)
{
  return iswupper (wc);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32isupper, int, (wint_t wc));
# endif
_GL_CXXALIAS_SYS (c32isupper, int, (wint_t wc));
_GL_CXXALIASWARN (c32isupper);
#endif
#if 1
# if (_GL_WCHAR_T_IS_UCS4 && !GNULIB_defined_mbstate_t) && !defined IN_C32ISXDIGIT
_GL_BEGIN_C_LINKAGE
_GL_INLINE int
c32isxdigit (wint_t wc)
{
  return iswxdigit (wc);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32isxdigit, int, (wint_t wc));
# endif
_GL_CXXALIAS_SYS (c32isxdigit, int, (wint_t wc));
_GL_CXXALIASWARN (c32isxdigit);
#endif


/* Case mapping of a 32-bit wide character.  */
#if 1
# if (_GL_WCHAR_T_IS_UCS4 && !GNULIB_defined_mbstate_t) && !defined IN_C32TOLOWER
_GL_BEGIN_C_LINKAGE
_GL_INLINE wint_t
c32tolower (wint_t wc)
{
  return towlower (wc);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32tolower, wint_t, (wint_t wc));
# endif
_GL_CXXALIAS_SYS (c32tolower, wint_t, (wint_t wc));
_GL_CXXALIASWARN (c32tolower);
#endif
#if 0
# if (_GL_WCHAR_T_IS_UCS4 && !GNULIB_defined_mbstate_t) && !defined IN_C32TOUPPER
_GL_BEGIN_C_LINKAGE
_GL_INLINE wint_t
c32toupper (wint_t wc)
{
  return towupper (wc);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32toupper, wint_t, (wint_t wc));
# endif
_GL_CXXALIAS_SYS (c32toupper, wint_t, (wint_t wc));
_GL_CXXALIASWARN (c32toupper);
#endif


/* Number of screen columns needed for a 32-bit wide character.  */
#if 1
# if (_GL_WCHAR_T_IS_UCS4 && !GNULIB_defined_mbstate_t) && !defined IN_C32WIDTH
_GL_BEGIN_C_LINKAGE
_GL_INLINE int
c32width (char32_t wc)
{
  return wcwidth (wc);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32width, int, (char32_t wc));
# endif
_GL_CXXALIAS_SYS (c32width, int, (char32_t wc));
_GL_CXXALIASWARN (c32width);
#endif


/* Converts a 32-bit wide character to a multibyte character.  */
#if 0
# if 0
#  if !(defined __cplusplus && defined GNULIB_NAMESPACE)
#   undef c32rtomb
#   define c32rtomb rpl_c32rtomb
#  endif
_GL_FUNCDECL_RPL (c32rtomb, size_t, (char *s, char32_t wc, mbstate_t *ps));
_GL_CXXALIAS_RPL (c32rtomb, size_t, (char *s, char32_t wc, mbstate_t *ps));
# else
#  if !1
_GL_FUNCDECL_SYS (c32rtomb, size_t, (char *s, char32_t wc, mbstate_t *ps));
#  endif
_GL_CXXALIAS_SYS (c32rtomb, size_t, (char *s, char32_t wc, mbstate_t *ps));
# endif
# if __GLIBC__ + (__GLIBC_MINOR__ >= 16) > 2
_GL_CXXALIASWARN (c32rtomb);
# endif
#elif defined GNULIB_POSIXCHECK
# undef c32rtomb
# if HAVE_RAW_DECL_C32RTOMB
_GL_WARN_ON_USE (c32rtomb, "c32rtomb is not portable - "
                 "use gnulib module c32rtomb for portability");
# endif
#endif


/* Convert a 32-bit wide string to a string.  */
#if 0
# if _GL_WCHAR_T_IS_UCS4 && !defined IN_C32SNRTOMBS
_GL_BEGIN_C_LINKAGE
_GL_INLINE _GL_ARG_NONNULL ((2)) size_t
c32snrtombs (char *dest, const char32_t **srcp, size_t srclen, size_t len,
             mbstate_t *ps)
{
  return wcsnrtombs (dest, (const wchar_t **) srcp, srclen, len, ps);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32snrtombs, size_t,
                  (char *dest, const char32_t **srcp, size_t srclen, size_t len,
                   mbstate_t *ps)
                  _GL_ARG_NONNULL ((2)));
# endif
_GL_CXXALIAS_SYS (c32snrtombs, size_t,
                  (char *dest, const char32_t **srcp, size_t srclen, size_t len,
                   mbstate_t *ps));
_GL_CXXALIASWARN (c32snrtombs);
#endif


/* Convert a 32-bit wide string to a string.  */
#if 0
# if _GL_WCHAR_T_IS_UCS4 && !defined IN_C32SRTOMBS
_GL_BEGIN_C_LINKAGE
_GL_INLINE _GL_ARG_NONNULL ((2)) size_t
c32srtombs (char *dest, const char32_t **srcp, size_t len, mbstate_t *ps)
{
  return wcsrtombs (dest, (const wchar_t **) srcp, len, ps);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32srtombs, size_t,
                  (char *dest, const char32_t **srcp, size_t len, mbstate_t *ps)
                  _GL_ARG_NONNULL ((2)));
# endif
_GL_CXXALIAS_SYS (c32srtombs, size_t,
                  (char *dest, const char32_t **srcp, size_t len,
                   mbstate_t *ps));
_GL_CXXALIASWARN (c32srtombs);
#endif


/* Convert a 32-bit wide string to a string.  */
#if 0
# if _GL_WCHAR_T_IS_UCS4 && !defined IN_C32STOMBS
_GL_BEGIN_C_LINKAGE
_GL_INLINE _GL_ARG_NONNULL ((2)) size_t
c32stombs (char *dest, const char32_t *src, size_t len)
{
  mbstate_t state;

  memset (&state, '\0', sizeof (mbstate_t));
  return c32srtombs (dest, &src, len, &state);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32stombs, size_t,
                  (char *dest, const char32_t *src, size_t len)
                  _GL_ARG_NONNULL ((2)));
# endif
_GL_CXXALIAS_SYS (c32stombs, size_t,
                  (char *dest, const char32_t *src, size_t len));
_GL_CXXALIASWARN (c32stombs);
#endif


/* Number of screen columns needed for a size-bounded 32-bit wide string.  */
#if 0
# if (_GL_WCHAR_T_IS_UCS4 && !GNULIB_defined_mbstate_t) && !defined IN_C32SWIDTH
_GL_BEGIN_C_LINKAGE
_GL_INLINE _GL_ARG_NONNULL ((1)) int
c32swidth (const char32_t *s, size_t n)
{
  return wcswidth ((const wchar_t *) s, n);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32swidth, int, (const char32_t *s, size_t n)
                                  _GL_ARG_NONNULL ((1)));
# endif
_GL_CXXALIAS_SYS (c32swidth, int, (const char32_t *s, size_t n));
_GL_CXXALIASWARN (c32swidth);
#endif


/* Converts a 32-bit wide character to unibyte character.
   Returns the single-byte representation of WC if it exists,
   or EOF otherwise.  */
#if 0
# if _GL_WCHAR_T_IS_UCS4 && !defined IN_C32TOB
_GL_BEGIN_C_LINKAGE
_GL_INLINE int
c32tob (wint_t wc)
{
  return wctob (wc);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (c32tob, int, (wint_t wc));
# endif
_GL_CXXALIAS_SYS (c32tob, int, (wint_t wc));
_GL_CXXALIASWARN (c32tob);
#endif


/* Converts a multibyte character to a 32-bit wide character.  */
#if 1
# if 1
#  if !(defined __cplusplus && defined GNULIB_NAMESPACE)
#   undef mbrtoc32
#   define mbrtoc32 rpl_mbrtoc32
#  endif
_GL_FUNCDECL_RPL (mbrtoc32, size_t,
                  (char32_t *pc, const char *s, size_t n, mbstate_t *ps));
_GL_CXXALIAS_RPL (mbrtoc32, size_t,
                  (char32_t *pc, const char *s, size_t n, mbstate_t *ps));
# else
#  if !1
_GL_FUNCDECL_SYS (mbrtoc32, size_t,
                  (char32_t *pc, const char *s, size_t n, mbstate_t *ps));
#  endif
_GL_CXXALIAS_SYS (mbrtoc32, size_t,
                  (char32_t *pc, const char *s, size_t n, mbstate_t *ps));
# endif
# if __GLIBC__ + (__GLIBC_MINOR__ >= 16) > 2
_GL_CXXALIASWARN (mbrtoc32);
# endif
#elif defined GNULIB_POSIXCHECK
# undef mbrtoc32
# if HAVE_RAW_DECL_MBRTOC32
_GL_WARN_ON_USE (mbrtoc32, "mbrtoc32 is not portable - "
                 "use gnulib module mbrtoc32 for portability");
# endif
#endif


/* Converts a multibyte character and returns the next 16-bit wide
   character.  */
#if 0
# if 0
#  if !(defined __cplusplus && defined GNULIB_NAMESPACE)
#   undef mbrtoc16
#   define mbrtoc16 rpl_mbrtoc16
#  endif
_GL_FUNCDECL_RPL (mbrtoc16, size_t,
                  (char16_t *pc, const char *s, size_t n, mbstate_t *ps));
_GL_CXXALIAS_RPL (mbrtoc16, size_t,
                  (char16_t *pc, const char *s, size_t n, mbstate_t *ps));
# else
#  if !1
_GL_FUNCDECL_SYS (mbrtoc16, size_t,
                  (char16_t *pc, const char *s, size_t n, mbstate_t *ps));
#  endif
_GL_CXXALIAS_SYS (mbrtoc16, size_t,
                  (char16_t *pc, const char *s, size_t n, mbstate_t *ps));
# endif
# if __GLIBC__ + (__GLIBC_MINOR__ >= 16) > 2
_GL_CXXALIASWARN (mbrtoc16);
# endif
#elif defined GNULIB_POSIXCHECK
# undef mbrtoc16
# if HAVE_RAW_DECL_MBRTOC16
_GL_WARN_ON_USE (mbrtoc16, "mbrtoc16 is not portable - "
                 "use gnulib module mbrtoc16 for portability");
# endif
#endif


/* Convert a string to a 32-bit wide string.  */
#if 0
# if _GL_WCHAR_T_IS_UCS4 && !defined IN_MBSNRTOC32S
_GL_BEGIN_C_LINKAGE
_GL_INLINE _GL_ARG_NONNULL ((2)) size_t
mbsnrtoc32s (char32_t *dest, const char **srcp, size_t srclen, size_t len,
             mbstate_t *ps)
{
  return mbsnrtowcs ((wchar_t *) dest, srcp, srclen, len, ps);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (mbsnrtoc32s, size_t,
                  (char32_t *dest, const char **srcp, size_t srclen, size_t len,
                   mbstate_t *ps)
                  _GL_ARG_NONNULL ((2)));
# endif
_GL_CXXALIAS_SYS (mbsnrtoc32s, size_t,
                  (char32_t *dest, const char **srcp, size_t srclen, size_t len,
                   mbstate_t *ps));
_GL_CXXALIASWARN (mbsnrtoc32s);
#endif


/* Convert a string to a 32-bit wide string.  */
#if 0
# if _GL_WCHAR_T_IS_UCS4 && !defined IN_MBSRTOC32S
_GL_BEGIN_C_LINKAGE
_GL_INLINE _GL_ARG_NONNULL ((2)) size_t
mbsrtoc32s (char32_t *dest, const char **srcp, size_t len, mbstate_t *ps)
{
  return mbsrtowcs ((wchar_t *) dest, srcp, len, ps);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (mbsrtoc32s, size_t,
                  (char32_t *dest, const char **srcp, size_t len, mbstate_t *ps)
                  _GL_ARG_NONNULL ((2)));
# endif
_GL_CXXALIAS_SYS (mbsrtoc32s, size_t,
                  (char32_t *dest, const char **srcp, size_t len,
                   mbstate_t *ps));
_GL_CXXALIASWARN (mbsrtoc32s);
#endif


/* Convert a string to a 32-bit wide string.  */
#if 0
# if _GL_WCHAR_T_IS_UCS4 && !defined IN_MBSTOC32S
_GL_BEGIN_C_LINKAGE
_GL_INLINE _GL_ARG_NONNULL ((2)) size_t
mbstoc32s (char32_t *dest, const char *src, size_t len)
{
  mbstate_t state;

  memset (&state, '\0', sizeof (mbstate_t));
  return mbsrtoc32s (dest, &src, len, &state);
}
_GL_END_C_LINKAGE
# else
_GL_FUNCDECL_SYS (mbstoc32s, size_t,
                  (char32_t *dest, const char *src, size_t len)
                  _GL_ARG_NONNULL ((2)));
# endif
_GL_CXXALIAS_SYS (mbstoc32s, size_t,
                  (char32_t *dest, const char *src, size_t len));
_GL_CXXALIASWARN (mbstoc32s);
#endif


_GL_INLINE_HEADER_END

#endif /* _GL_UCHAR_H */
