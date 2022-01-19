// Copyright (C) <2020> Intel Corporation
//
// SPDX-License-Identifier: Apache-2

// clang-format off
#include "rapidjson/document.h"
#include "rapidjson/stringbuffer.h"
#include "rapidjson/writer.h"
#include "controlhandler.h"
// clang-format on

std::string InputEventHandler::OnKeyboardEvent(KeyboardOptions *p_key_options) {
  WPARAM wparam = p_key_options->v_key_;
  UINT msg = p_key_options->msg_;
  rapidjson::Document event;
  event.SetObject();
  rapidjson::Document::AllocatorType &alloc = event.GetAllocator();

  rapidjson::Value parameters(rapidjson::kObjectType);
  parameters.SetObject();
  parameters.AddMember("which", wparam, alloc);

  rapidjson::Value data(rapidjson::kObjectType);
  switch (msg) {
    case WM_KEYDOWN:
      data.AddMember("event", "keydown", alloc);
      break;
    case WM_KEYUP:
      data.AddMember("event", "keyup", alloc);
      break;
    default:
      break;
  }
  data.AddMember("parameters", parameters, alloc);
  event.AddMember("type", "control", alloc);
  event.AddMember("data", data, alloc);

  rapidjson::StringBuffer buffer;
  rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
  event.Accept(writer);
  return buffer.GetString();
}

std::string InputEventHandler::OnMouseEvent(MouseOptions *p_m_options,
                                           bool is_raw) {
  rapidjson::Document event;
  event.SetObject();
  rapidjson::Document::AllocatorType& alloc = event.GetAllocator();

  int x = p_m_options->x_pos_;
  int y = p_m_options->y_pos_;
  rapidjson::Value parameters(rapidjson::kObjectType);
  parameters.SetObject();

  parameters.AddMember("x", x, alloc);
  parameters.AddMember("y", y, alloc);
  parameters.AddMember("movementX", p_m_options->x_pos_, alloc);
  parameters.AddMember("movementY", p_m_options->y_pos_, alloc);

  rapidjson::Value data(rapidjson::kObjectType);
  switch (p_m_options->m_event_) {
  case kMouseMove:
    data.AddMember("event", "mousemove", alloc);
    break;
  case kMouseLeftButton:
    if (p_m_options->m_button_state_ == kMouseButtonDown) {
      data.AddMember("event", "mousedown", alloc);
      parameters.AddMember("which", 1, alloc);
    } else {
      data.AddMember("event", "mouseup", alloc);
      parameters.AddMember("which", 1, alloc);
    }
    break;
  case kMouseMiddleButton:
    if (p_m_options->m_button_state_ == kMouseButtonDown) {
      data.AddMember("event", "mousedown", alloc);
      parameters.AddMember("which", 2, alloc);
    } else {
      data.AddMember("event", "mouseup", alloc);
      parameters.AddMember("which", 2, alloc);
    }
    break;
  case kMouseRightButton:
    if (p_m_options->m_button_state_ == kMouseButtonDown) {
      data.AddMember("event", "mousedown", alloc);
      parameters.AddMember("which", 3, alloc);
    } else {
      data.AddMember("event", "mouseup", alloc);
      parameters.AddMember("which", 3, alloc);
    }
    break;
  case kMouseWheel:
    data.AddMember("event", "wheel", alloc);
  // Javascript client allows deltaX and deltaZ to be set.. so we have
  // to set them to 0, otherwise ga server will crash.
  parameters.AddMember("deltaX", 0, alloc);
    parameters.AddMember("deltaY", p_m_options->delta_y_, alloc);
  parameters.AddMember("deltaZ", 0, alloc);
    break;
  default:
    break;
  }
  data.AddMember("parameters", parameters, alloc);
  event.AddMember("type", "control", alloc);
  event.AddMember("data", data, alloc);

  rapidjson::StringBuffer buffer;
  rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);

  event.Accept(writer);
  return buffer.GetString();
}
