<?php

/* 
 * Stardict PHP definition wrapper - enables the software to use a .dict
 * file without having it on his device (useful for embedded systems)
 * 
 * Copyright (C) 2009 Alexis ROBERT <alexis.robert@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, at version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

header("Content-Type: text/html;charset=utf-8");

$offset = intval($_GET['offset']);
$size = intval($_GET['size']);

if ($offset < 0 || $size <= 0)
	die("Bad offset/size");

$f = fopen("XMLittre.dict",'r');
fseek($f,$offset);
print nl2br(fread($f,$size));

?>
