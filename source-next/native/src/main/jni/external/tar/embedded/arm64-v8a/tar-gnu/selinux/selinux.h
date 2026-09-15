/* DO NOT EDIT! GENERATED AUTOMATICALLY! */
/* Replacement <selinux/selinux.h> for platforms that lack it.
   Copyright (C) 2008-2023 Free Software Foundation, Inc.

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


#if 0

#include_next 

#else
# if !defined _GL_SELINUX_SELINUX_H
#  define _GL_SELINUX_SELINUX_H

/* This file uses _GL_INLINE_HEADER_BEGIN, _GL_INLINE,
   _GL_ATTRIBUTE_MAYBE_UNUSED.  */
#  if !_GL_CONFIG_H_INCLUDED
#   error "Please include config.h first."
#  endif

#  include <sys/types.h>
#  include <errno.h>

_GL_INLINE_HEADER_BEGIN
#  ifndef SE_SELINUX_INLINE
#   define SE_SELINUX_INLINE _GL_INLINE
#  endif

/* _GL_ATTRIBUTE_MAYBE_UNUSED declares that it is not a programming mistake if
   the entity is not used.  The compiler should not warn if the entity is not
   used.  */
#  ifndef _GL_ATTRIBUTE_MAYBE_UNUSED
#   if 0 /* no GCC or clang version supports this yet */
#    define _GL_ATTRIBUTE_MAYBE_UNUSED [[__maybe_unused__]]
#   elif defined __GNUC__ || defined __clang__
#    define _GL_ATTRIBUTE_MAYBE_UNUSED __attribute__ ((__unused__))
#   else
#    define _GL_ATTRIBUTE_MAYBE_UNUSED
#   endif
#  endif

#  if !GNULIB_defined_security_types

typedef unsigned short security_class_t;
struct selinux_opt;
#   define is_selinux_enabled() 0

SE_SELINUX_INLINE int
getcon (_GL_ATTRIBUTE_MAYBE_UNUSED char **con)
  { errno = ENOTSUP; return -1; }
SE_SELINUX_INLINE void
freecon (_GL_ATTRIBUTE_MAYBE_UNUSED char *con) {}

SE_SELINUX_INLINE int
getfscreatecon (_GL_ATTRIBUTE_MAYBE_UNUSED char **con)
  { errno = ENOTSUP; return -1; }
SE_SELINUX_INLINE int
setfscreatecon (_GL_ATTRIBUTE_MAYBE_UNUSED char const *con)
  { errno = ENOTSUP; return -1; }
SE_SELINUX_INLINE int
matchpathcon (_GL_ATTRIBUTE_MAYBE_UNUSED char const *file,
              _GL_ATTRIBUTE_MAYBE_UNUSED mode_t m,
              _GL_ATTRIBUTE_MAYBE_UNUSED char **con)
  { errno = ENOTSUP; return -1; }
SE_SELINUX_INLINE int
getfilecon (_GL_ATTRIBUTE_MAYBE_UNUSED char const *file,
            _GL_ATTRIBUTE_MAYBE_UNUSED char **con)
  { errno = ENOTSUP; return -1; }
SE_SELINUX_INLINE int
lgetfilecon (_GL_ATTRIBUTE_MAYBE_UNUSED char const *file,
             _GL_ATTRIBUTE_MAYBE_UNUSED char **con)
  { errno = ENOTSUP; return -1; }
SE_SELINUX_INLINE int
fgetfilecon (int fd,_GL_ATTRIBUTE_MAYBE_UNUSED char **con)
  { errno = ENOTSUP; return -1; }
SE_SELINUX_INLINE int
setfilecon (_GL_ATTRIBUTE_MAYBE_UNUSED char const *file,
            _GL_ATTRIBUTE_MAYBE_UNUSED char const *con)
  { errno = ENOTSUP; return -1; }
SE_SELINUX_INLINE int
lsetfilecon (_GL_ATTRIBUTE_MAYBE_UNUSED char const *file,
             _GL_ATTRIBUTE_MAYBE_UNUSED char const *con)
  { errno = ENOTSUP; return -1; }
SE_SELINUX_INLINE int
fsetfilecon (_GL_ATTRIBUTE_MAYBE_UNUSED int fd,
             _GL_ATTRIBUTE_MAYBE_UNUSED char const *con)
  { errno = ENOTSUP; return -1; }

SE_SELINUX_INLINE int
security_check_context (_GL_ATTRIBUTE_MAYBE_UNUSED char const *con)
  { errno = ENOTSUP; return -1; }
SE_SELINUX_INLINE int
security_check_context_raw (_GL_ATTRIBUTE_MAYBE_UNUSED char const *con)
  { errno = ENOTSUP; return -1; }
SE_SELINUX_INLINE int
setexeccon (_GL_ATTRIBUTE_MAYBE_UNUSED char const *con)
  { errno = ENOTSUP; return -1; }
SE_SELINUX_INLINE int
security_compute_create (_GL_ATTRIBUTE_MAYBE_UNUSED char const *scon,
                         _GL_ATTRIBUTE_MAYBE_UNUSED char const *tcon,
                         _GL_ATTRIBUTE_MAYBE_UNUSED security_class_t tclass,
                         _GL_ATTRIBUTE_MAYBE_UNUSED char **newcon)
  { errno = ENOTSUP; return -1; }
SE_SELINUX_INLINE security_class_t
string_to_security_class (char const *name)
  { errno = ENOTSUP; return 0; }
SE_SELINUX_INLINE int
matchpathcon_init_prefix (_GL_ATTRIBUTE_MAYBE_UNUSED char const *path,
                          _GL_ATTRIBUTE_MAYBE_UNUSED char const *prefix)
  { errno = ENOTSUP; return -1; }

#   define GNULIB_defined_security_types 1
#  endif

_GL_INLINE_HEADER_END

# endif
#endif
