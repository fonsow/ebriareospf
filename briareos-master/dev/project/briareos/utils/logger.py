from briareos.common import *

from colored import fg, attr

import random
import logging


logging.basicConfig(filename=BRIAREOS_LOG_PATH, level=logging.INFO,
                    format="%(asctime)s %(levelname)s:%(message)s", datefmt="%m/%d/%Y %I:%M:%S %p")


def success(s):
    print("[%s+%s] %s%s%s%s" % (fg("light_green"), attr(0), attr(1), s, attr(21), attr(0)))
    logging.info(s)


def info(s):
    print("[%s*%s] %s" % (fg("light_cyan"), attr(0), s))
    logging.info(s)


def subinfo(s):
    info("-> %s" % s)
    logging.info(s)


def subsubinfo(s):
    info("---> %s" % s)
    logging.info(s)


def warning(s):
    print("[%s!%s] %s" % (fg("yellow"), attr(0), s))
    logging.warning(s)


def subwarning(s):
    warning("-> %s" % s)
    logging.warning(s)


def error(s):
    print("[%sX%s] %s%s%s%s" % (fg("red"), attr(0), attr(1), s, attr(21), attr(0)))
    logging.error(s)


def suberror(s):
    error("-> %s" % s)
    logging.error(s)


def debug(s):
    print("[DEBUG] %s" % s)


def banner(info, version):
    padding = 5
    info_version = text_bold("%s || v%s" % (info, version))
    banner_content = "%s %s %s" % (text_random_colors("#"*padding), info_version, text_random_colors("#"*padding))
    banner = text_random_colors("#"*(padding*2 + len(info) + len(str(version)) + 7))
    print(banner)
    print(banner_content)
    print(banner + "\n")


def text_random_colors(s):
    output = ""
    for c in s:
        output += fg(random.randint(1, 230)) + c
    output += attr(0)
    return output

def text_bold(s):
    return "%s%s%s%s" % (attr(1), s, attr(21), attr(0))
