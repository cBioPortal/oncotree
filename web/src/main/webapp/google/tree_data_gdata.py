#! /usr/bin/env python

# ------------------------------------------------------------------------------
# Script which get Oncotree data from google spreadsheet
# ------------------------------------------------------------------------------
# imports
import gdata.spreadsheet.service
import sys
# ------------------------------------------------------------------------------
# globals

# some file descriptors

GOOGLE_ID = sys.argv[1]
GOOGLE_PW = sys.argv[2]
SPREADSHEETID = '0AhyhUieStXV-dFdXbGZ6dGFIRE0tWTJ5RGpYV2E5Y2c'
ENTRYNAME = 'oncotree_src'
ENTRYINDEX = 2
LELVENAMES = ['primary', 'secondary', 'tertiary', 'quaternary', 'quniary'];
# EMAILFROM = 'jackson.zhang.828@gmail.com'
# EMAILSUBJECT = 'Auto email script is ready, please send me the offical email subject.'
# EMAILTEMPLATEPATH = 'email_template.txt'

# a ref to the google spreadsheet client - used for all i/o to google spreadsheet
GOOGLE_SPREADSHEET_CLIENT = gdata.spreadsheet.service.SpreadsheetsService()


# ------------------------------------------------------------------------------

# logs into google spreadsheet client

def google_login(user, pw):
  # google spreadsheet
  GOOGLE_SPREADSHEET_CLIENT.ClientLogin(user, pw)

# ------------------------------------------------------------------------------
# gets a worksheet feed

def get_worksheet_feed():
  return GOOGLE_SPREADSHEET_CLIENT.GetWorksheetsFeed(SPREADSHEETID)

# ------------------------------------------------------------------------------
# gets a cells feed

def get_cells_feed():
  return GOOGLE_SPREADSHEET_CLIENT.GetCellsFeed(SPREADSHEETID)


# ------------------------------------------------------------------------------
# gets a list feed

def get_list_feed():
  worksheets_feed = get_worksheet_feed()
  # print worksheets_feed
  for entry in worksheets_feed.entry:
    if entry.title.text == ENTRYNAME:
      worksheet_id = entry.id.text.rsplit('/',1)[1]
      return GOOGLE_SPREADSHEET_CLIENT.GetListFeed(SPREADSHEETID, worksheet_id)

# ------------------------------------------------------------------------------
# the big deal main.

def main():

  # login to google and get spreadsheet feed
  google_login(GOOGLE_ID, GOOGLE_PW)
  print 'Successed connect to Google Driv API.'
  cells_feed = get_list_feed()

  print 'Got Onco Tree data.'
  treeData = open('tumor_tree.txt', 'w')

  fileHeader = '\t'.join(LELVENAMES)

  treeData.write(fileHeader.strip() + '\n')
  # Loop through the feed and extract each document entry.
  for entry in cells_feed.entry:
    tempDict = {}
    for key in entry.custom:
      variable = entry.custom[key].text
      if variable is None:
        tempDict[key] = ''
      else:
        tempDict[key] = variable.encode('utf-8')

    s = ''

    for levelName in LELVENAMES:
      if levelName in tempDict:
        s += tempDict[levelName] + '\t'

    s = s.strip()
    treeData.write(s + '\n')
  treeData.close()

  print 'Data file updated successfully.'

# ------------------------------------------------------------------------------
# ready to import data from google drive
main()
